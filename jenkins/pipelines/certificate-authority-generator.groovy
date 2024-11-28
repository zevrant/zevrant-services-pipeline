@Library("CommonUtils") _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.NotificationService

import java.nio.charset.StandardCharsets

KubernetesService kubernetesService = new KubernetesService(this)
CertificateService certificateService = new CertificateService(this)

if (ENVIRONMENT == '' || ENVIRONMENT == null) {
    throw new RuntimeException("Environment name must be provided, exiting...")
}

String caLabel = 'app=certificate-authority'
String environmentTitle = ENVIRONMENT.split('-').collect({ name -> name.capitalize() }).join(' ')
pipeline {
    agent {
        kubernetes {
            inheritFrom 'cert-manager'
        }
    }

    stages {

        stage('Erase Existing') {
            when { expression { Boolean.valueOf(REMOVE_EXISTING) } }
            steps {
                script {
                    container('kubectl') {
                        String pod = kubernetesService.getPodName(caLabel, ENVIRONMENT as String)
                        sh "kubectl exec -n $ENVIRONMENT -i $pod -- rm -rf /opt/step-ca/.step"
                        sh "kubectl rollout restart deploy -n $ENVIRONMENT certificate-authority"
                        sh "kubectl rollout status deploy -n $ENVIRONMENT certificate-authority"
                    }
                }
            }
        }

        stage('Generate CSR') {
            steps {
                script {
                    container('kubectl') {
                        String pod = kubernetesService.getPodName('app=certificate-authority', ENVIRONMENT as String)
                        sh "kubectl exec -n $ENVIRONMENT -i $pod -- rm -f /opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/secrets/root_ca_key"
                        sh "kubectl exec -n $ENVIRONMENT -i $pod -- rm -f /opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/certs/root_ca.crt"
                        retry(3, {
                            try {
                                sh "kubectl exec -n $ENVIRONMENT -i $pod -- ls -l '/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/secrets/'"
                            } catch (Exception ignored) {
                                sleep 6
                                throw ignored
                            }
                        })

                        sh "kubectl exec -n $ENVIRONMENT -i $pod -- step certificate create 'Zevrant Services ${environmentTitle}' '/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/certs/intermediate_ca.csr' '/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/secrets/intermediate_ca_key' --csr --san certificate-authority.${ENVIRONMENT}.svc.cluster.local --password-file /var/zevrant-services/ca-password/password --force"

                        sh "kubectl cp -n $ENVIRONMENT '${pod}:/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/certs/intermediate_ca.csr' ./intermediate_ca.csr"
                    }
                }
            }
        }

        stage('Pull CA & Decrypt') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                script {
                    container('cert-manager') {
                        sh 'aws secretsmanager get-secret-value --secret-id /root/ca/envryption-properties > secret.json'
                        def secret = readJSON(file: 'secret.json')
                        def secretValues = readJSON(text: secret.SecretString.replace('\\"', '"'))
                        writeFile(file: 'salt', text: secretValues.salt)
                        writeFile(file: 'iv', text: secretValues.iv)
                        writeFile(file: 'symmetric-key-password', text: secretValues['symmetric-password'])
                        writeFile(file: 'symmetric-key.enc.b64', text: secretValues['symmetric-key'])
                        sh 'aws s3 cp s3://zevrant-artifact-store/ca/rootCA.zip.enc ./rootCA.zip.enc'
                        sh 'aws s3 cp s3://zevrant-artifact-store/ca/private.pem ./private.pem'
                        sh 'aws s3 cp s3://zevrant-artifact-store/ca/public.pem ./public.pem'

                        sh 'cat symmetric-key.enc.b64 | base64 --decode > symmetric.key.enc'
                        sh 'openssl rsautl -decrypt -inkey private.pem -in symmetric.key.enc -out symmetric.key'

                        sh 'set +x; openssl enc -aes-256-cbc -d -iter 1000000 -K "$(cat symmetric.key)" -in rootCA.zip.enc --pass file:symmetric-key-password -out rootCA.zip -iv "$(cat iv)" -S "$(cat salt)"'

                    }
                }
            }
        }

        stage('Issue Certificate') {
            steps {
                script {
                    container('cert-manager') {
                        sh 'unzip rootCA.zip'
                        dir('rootCA') {
                            writeFile(file: 'issued/openssl.conf', text: readFile(file: 'issued/openssl.conf').replace('dir             = /home/zevrant/rootCA', "dir             = ${pwd()}"))
                            if (fileExists("issued/${ENVIRONMENT}.crt")) {
                                sh "openssl ca -config issued/openssl.conf -revoke issued/${ENVIRONMENT}.crt"
                                sh "rm issued/${ENVIRONMENT}.*"
                            }
                            sh "mv ../intermediate_ca.csr issued/${ENVIRONMENT}.csr"
                            String opensslConf = certificateService.addSan(readFile(file: 'issued/openssl.conf') as String, '[ policy_loose ]', "certificate-authority.${ENVIRONMENT}.svc.cluster.local")

                            println opensslConf
                            sh 'mv issued/openssl.conf issued/openssl.conf.bak'
                            writeFile(file: 'issued/openssl.conf', text: opensslConf)
                            sh "openssl ca -batch -config issued/openssl.conf -extensions v3_intermediate_ca -days 1825 -in issued/${ENVIRONMENT}.csr -out issued/${ENVIRONMENT}.crt "
                            archiveArtifacts("issued/${ENVIRONMENT}.crt")
                            sh 'mv issued/openssl.conf.bak issued/openssl.conf'
                            sh "cp issued/${ENVIRONMENT}.crt ../intermediate_ca.crt"
                            sh "cp ca.crt ../ca.crt"

                        }
                        sh 'rm rootCA.zip; zip -r rootCA.zip rootCA/*'
                    }
                }
            }
        }

        stage('Encrypt & Push') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                script {
                    container('cert-manager') {
                        sh 'set +x; openssl enc -aes-256-cbc -iter 1000000 -K "$(cat symmetric.key)" -in rootCA.zip --pass file:symmetric-key-password -out rootCA.zip.enc -iv "$(cat iv)" -S "$(cat salt)"'
                        sh 'aws s3 cp rootCA.zip.enc s3://zevrant-artifact-store/ca/rootCA.zip.enc'
                        sh 'step certificate fingerprint ca.crt > fingerprint'
                        archiveArtifacts('fingerprint')
                    }
                }
            }
        }

        stage('Container Upload') {
            steps {
                script {
                    container('kubectl') {
                        String pod = kubernetesService.getPodName('app=certificate-authority', ENVIRONMENT as String)
                        writeFile(file: 'intermediate_ca.crt', text: certificateService.cleanCert(readFile(file: 'intermediate_ca.crt') as String))

                        sh "kubectl cp -n $ENVIRONMENT ./intermediate_ca.crt '${pod}:/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/certs/intermediate_ca.crt'"
                        sh "kubectl cp -n $ENVIRONMENT ./ca.crt '${pod}:/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/certs/root_ca.crt'"
                        sh "kubectl cp -n $ENVIRONMENT '${pod}:/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/config/ca.json' ./ca.json"
                        String caConfig = readFile(file: 'ca.json')
                        caConfig = caConfig.replace('"certificate-authority"', "\"certificate-authority.${ENVIRONMENT}.svc.cluster.local\"")
                        writeFile(file: 'ca.json', text: caConfig)
                        sh "kubectl cp -n $ENVIRONMENT ./ca.json '${pod}:/opt/step-ca/.step/authorities/Zevrant Services ${environmentTitle}/config/ca.json'"
                        sh "kubectl -n $ENVIRONMENT rollout restart deployment certificate-authority"
                        sh "kubectl -n $ENVIRONMENT rollout status deployment certificate-authority"
                        pod = kubernetesService.getPodName('app=certificate-authority', ENVIRONMENT as String)
                        String fingerprint = readFile(file: 'fingerprint')
                        writeFile(file: 'ca-fingerprint.yml', text: """
apiVersion: v1
data:
  fingerprint: ${Base64.getEncoder().encodeToString(fingerprint.getBytes(StandardCharsets.UTF_8))}
kind: Secret
metadata:
  name: ca-fingerprint
  namespace: $ENVIRONMENT
type: Opaque
""")
                        sh 'cat ca-fingerprint.yml; kubectl apply -f ca-fingerprint.yml'
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                sh 'rm -rf ./*'
            }
        }
        success {
            script {
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    new NotificationService(this).sendDiscordNotification(
                            "Jenkins Successfully Created The CA Certs for the $environmentTitle Environment",
                            env.BUILD_URL,
                            currentBuild.currentResult,
                            "CA Certificate Generation",
                            NotificationChannel.DISCORD_CICD
                    )
                }
            }
        }
    }
}