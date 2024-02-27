import com.zevrant.services.services.KubernetesService

@Library('CommonUtils')

KubernetesService kubernetesService = new KubernetesService(this)

String keystore

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
        stage('Get Certificates') {
            steps {
                script {
                    container('kubectl') {
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
            steps {
                script {
                    container('jnlp') {
                        sh 'openssl pkcs12 -export -inkey tls.key -in tls.crt -passout \'file:password\' -out zevrant-services.p12'
                        sh 'cat zevrant-services.p12 | base64 > keystore.b64'
                        keystore = readFile(file: 'keystore.b64')
                    }
                }
            }
        }
    }
}


node('master-node') {
    stage('Reload Certificate') {
        if (keystore == null || keystore.isBlank()) {
            withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                discordSend description: "Jenkins failed to rotate certs for itself", result: currentBuild.currentResult, title: "Certificate Rotation", webhookURL: webhookUrl
            }
            throw new RuntimeException("Failed to rotate jenkins certificates, null keystore provided")
        }
        writeFile(file: 'keystore.b64', text: keystore)
        sh 'cat keystore.b64 | base64 -d > /etc/pki/jenkins/zevrant-services.p12 && rm keystore.b64'
    }

    stage('Restart Jenkins') {
        Jenkins.instance.doSafeExit(null);
    }
}
