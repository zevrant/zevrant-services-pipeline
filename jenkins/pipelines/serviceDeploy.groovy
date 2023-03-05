import com.zevrant.services.ServiceLoader
import com.zevrant.services.pojo.KubernetesService
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.GitService

KubernetesService service = KubernetesServiceCollection.findServiceByName(SERVICE_NAME as String) as KubernetesService
GitService gitService = ServiceLoader.load(binding, GitService.class) as GitService
CertificateService certificateService = ServiceLoader.load(binding, CertificateService.class) as CertificateService

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }
    options {
        retry(3)
    }
    stages {
        stage("SCM Checkout") {
            steps {
                container('kubectl') {
                    script {
                        retry {
                            try {
                                gitService.checkout('zevrant-services-pipeline')
                            } catch (Exception ex) {
                                println "Sleeping before retry"
                                sleep 5
                                throw ex
                            }
                        }

                    }
                }
            }
        }
        stage("Deploy Service") {
            steps {
                container('kubectl') {
                    script {
                        Map<String, Object> services = [
                                'Deploy Service': {
                                    sh "kubectl rollout restart deployment $SERVICE_NAME -n $ENVIRONMENT"
                                    sh "kubectl rollout status deployments $SERVICE_NAME -n $ENVIRONMENT --timeout=5m"
                                }
                        ]

                        if (service.includesDb) {
                            services.put('Deploy Service Database', {
                                sh "kubectl get deployment ${SERVICE_NAME}-db -n $ENVIRONMENT"
                                sh "kubectl rollout status deployments ${SERVICE_NAME}-db -n $ENVIRONMENT --timeout=5m"
                            })
                        }

                        parallel services
                    }
                }
            }
        }

        stage('Certificate Check') {
            steps {
                script {
                    container('kubectl') {
                        String url = service.url ?: certificateService.getMicroserviceUrl(SERVICE_NAME as String, ENVIRONMENT as String)
                        retry(3, {

                            if (!certificateService.isCertificateValid(url)) {
                                throw new RuntimeException("Certificate expirred for ${SERVICE_NAME}, certs were not rotated")
                            }
                        })
                    }
                }
            }
        }
    }
    post {
        failure {
            script {
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Service Push to ${ENVIRONMENT.toLowerCase().capitalize()} for KubernetesService.groovy Service ${SERVICE_NAME}: ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "KubernetesService.groovy Service Deploy", webhookURL: webhookUrl
                }
            }
        }
    }
}

