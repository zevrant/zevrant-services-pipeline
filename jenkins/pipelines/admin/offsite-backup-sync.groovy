@Library('CommonUtils') _

String keystore

pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Synchronize T0 Backups') {
            steps {
                script {

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


//node('master-node') {
//    stage('Reload Certificate') {
//        if (keystore == null || keystore.isBlank()) {
//            new NotificationService(this).sendDiscordNotification(
//                    "Jenkins failed to rotate certs for itself",
//                    env.BUILD_URL,
//                    currentBuild.currentResult,
//                    "Certificate Rotation",
//                    NotificationChannel.DISCORD_CICD
//            )
//            throw new RuntimeException("Failed to rotate jenkins certificates, null keystore provided")
//        }
//        writeFile(file: 'keystore.b64', text: keystore)
//        sh 'cat keystore.b64 | base64 -d > /etc/pki/jenkins/zevrant-services.p12 && rm keystore.b64'
//    }
//
//    stage('Restart Jenkins') {
//        Jenkins.instance.doSafeRestart(null, "Rotating Certificates");
//    }
//}
