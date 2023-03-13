package admin

List<String> hosts = [
        'raspi-04-01.zevrant-services.com',
        // 'raspi-04-02.zevrant-services.com'
]

if(ENVIRONMENT == null || ENVIRONMENT == '') {
    throw new RuntimeException('Environment name must be provided.')
}

if(DOMAIN == null || DOMAIN == '') {
    throw new RuntimeException('Domain name must be provided.')
}


pipeline {
    agent {
        kubernetes {
            inheritFrom 'certbot'
        }
    }

    stages {
        stage('Get Certs') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                script {
                    container('certbot') {
                        sh "certbot -v certonly --dns-route53 -d ${DOMAIN} --non-interactive --agree-tos -m gdittrick@zevrant-services.com"
                        sh "cp /etc/letsencrypt/live/${DOMAIN}/privkey.pem privkey.pem"
                        sh "cp /etc/letsencrypt/live/${DOMAIN}/fullchain.pem fullchain.pem"
                        sh 'chmod a+r privkey.pem'
                        sh 'ls -la'
                        sh 'id -u'

                    }
                }
            }
        }

        stage("Deploy Certificates") {
            steps {
                script {
                    container('kubectl') {
                        try {
                            sh 'kubectl delete secrets ui-core-tls -n $ENVIRONMENT'
                        } catch (Exception ignored) {
                            println 'secret doesnt exist continuing'
                        }

                        sh "kubectl create secret generic ui-core-tls -n $ENVIRONMENT --from-file=tls.crt=fullchain.pem --from-file=tls.key=privkey.pem"
                    }
                }
            }
        }
    }
}