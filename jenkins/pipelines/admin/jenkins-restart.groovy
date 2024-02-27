import com.zevrant.services.services.KubernetesService

@Library('CommonUtils')

KubernetesService kubernetesService = new KubernetesService(this)

byte[] keystore

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
        stage('Get Certificates') {
            container('kubectl') {
                steps {
                    script {
                        String tlsCrt = kubernetesService.getSecretValue('jenkins-internal-tls', 'tls.crt', 'jenkins')
                        String tlsKey = kubernetesService.getSecretValue('jenkins-internal-tls', 'tls.key', 'jenkins')
                        String tlsPassword = kubernetesService.getSecretValue('jenkins-keystore-password', 'password', 'jenkins')

                        writeFile(file: 'tls.crt', text: tlsCrt)
                        writeFile(file: 'tls.key', text: tlsKey)
                        writeFile(file: 'password', text: tlsPassword)
                    }
                }
            }
        }

        stage('Create P12 Keystore') {
            container('jnlp') {
                steps {
                    script {
                        sh 'openssl pkcs12 -export -inkey tls.key -in tls.crt -passout \'file:password\' -out zevrant-services.p12'
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(new File('zevrant-services.p12')))
                        keystore = bufferedInputStream.readAllBytes()
                    }
                }
            }
        }
    }
}


node('master-node') {
    stage('Reload Certificate') {
        if (keystore == null || keystore.length == 0) {
            withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                discordSend description: "Jenkins failed to rotate certs for itself", result: currentBuild.currentResult, title: "Certificate Rotation", webhookURL: webhookUrl
            }
            throw new RuntimeException("Failed to rotate jenkins certificates, null keystore provided")
        }
        BufferedOutputStream outputStream = new BufferedWriter(new FileOutputStream(new File('/etc/pki/jenkins/zevrant-services.p12')))
        outputStream.write(keystore)
    }

    stage('Restart Jenkins') {
        Jenkins.instance.doSafeExit(null);
    }
}
