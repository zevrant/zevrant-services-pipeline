import com.zevrant.services.ServiceLoader
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.KubernetesService

CertificateService certificateService = ServiceLoader.load(binding, CertificateService.class) as CertificateService
KubernetesService kubernetesService = ServiceLoader.load(binding, KubernetesService.class) as KubernetesService

LinkedHashMap<String, Serializable> serviceNameOverrides = [
        'develop-zevrant-home-ui'     : '172.16.1.10',
        'develop-zevrant-home-ui-port': 30124
] as LinkedHashMap<String, Serializable>

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }
    stages {
        stage("SCM Checkout") {
            when { expression { VERSION != null && VERSION != '' } }
            steps {
                container('kubectl') {
                    script {
                        currentBuild.displayName = "Deploying Version $VERSION"
                        git credentialsId: 'jenkins-git', branch: 'master',
                                url: "ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/${REPOSITORY}.git" as String
                    }
                }
            }
        }
        stage("Deploy Database") {
            when { expression { fileExists('database.yml') && VERSION != null && VERSION != '' } }
            steps {
                container('kubectl') {
                    script {
                        sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./database.yml"
                        sh "kubectl apply -n $ENVIRONMENT -f ./database.yml"
                        try {
                            sh "kubectl get deployment $REPOSITORY-db -n $ENVIRONMENT "
                        } catch (Exception ignored) {
                            throw new RuntimeException("Failed to retrieve deployment for repository ${REPOSITORY}-db in $ENVIRONMENT")
                        }

//                        try {
                            sh "kubectl rollout status deployments ${REPOSITORY}-db -n $ENVIRONMENT --timeout=5m"
//                        } catch (Exception ignored) {
//                            sh "kubectl rollout undo deploy ${REPOSITORY}-db -n $ENVIRONMENT"
//                            throw new RuntimeException("Deployment for ${REPOSITORY}-db in Environment $ENVIRONMENT failed and was rolled back")
//                        }
                    }
                }
            }
        }

        stage ("Configure Service") {
            when { expression { fileExists("service-${ENVIRONMENT}.yml") } }
            steps {
                script {
                    container('kubectl') {
                        sh "kubectl apply -f service-${ENVIRONMENT}.yml -n ${ENVIRONMENT}"
                    }
                }
            }
        }

        stage("Deploy Micro Service") {
            when { expression { fileExists('deployment.yml') && VERSION != null && VERSION != '' } }
            steps {
                container('kubectl') {
                    script {
                        sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./deployment.yml"
                        sh "sed -i 's/\$VERSION/$VERSION/g' ./deployment.yml"
                        sh "sed -i 's/\$REPLICAS/${ENVIRONMENT == 'prod' ? 2 : 1}/g' ./deployment.yml"
                        String deploymentText = ((String) readFile(file: 'deployment.yml'))
                        println(deploymentText)
                        int timeout = kubernetesService.getDeployTimeout(ENVIRONMENT == 'prod' ? 2 : 1)
                        sh "kubectl apply -n $ENVIRONMENT -f ./deployment.yml"
                        String deploymentName = ""
                        try {
                            sh "kubectl get deployment $REPOSITORY -n $ENVIRONMENT"
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to retrieve deployment for repository $REPOSITORY in $ENVIRONMENT")
                        }
//                        try {
                            sh "kubectl rollout status deployments $REPOSITORY -n $ENVIRONMENT --timeout=${timeout}s"
//                        } catch (Exception ignored) {
//                            sh "kubectl rollout undo deploy $REPOSITORY -n $ENVIRONMENT"
//                            throw new RuntimeException("Deployment for $REPOSITORY in Environment $ENVIRONMENT failed and was rolled back")
//                        }
                    }
                }
            }
        }

        stage("Rotate Certificates") {
            when { expression { VERSION == '' || VERSION == null } }
            steps {
                script {
                    retry(3, {
                        container('kubectl') {
                            try {
                                sh "kubectl get deployment $REPOSITORY -n $ENVIRONMENT"
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to retrieve deployment for repository $REPOSITORY in $ENVIRONMENT")
                            }
                            sh "kubectl rollout restart deployments $REPOSITORY -n $ENVIRONMENT"
                            int timeout = kubernetesService.getDeployTimeout(ENVIRONMENT == 'prod' ? 2 : 1)
//                            try {
                                sh "kubectl rollout status deployments $REPOSITORY -n $ENVIRONMENT --timeout=${timeout}s"
//                            } catch (Exception ignored) {
//                                sh "kubectl rollout undo deploy $REPOSITORY -n $ENVIRONMENT"
//                                throw new RuntimeException("Deployment for $REPOSITORY in Environment $ENVIRONMENT failed and was rolled back")
//                            }
                            boolean isCertValid = false
                            String serviceKey = "${ENVIRONMENT}-${REPOSITORY}"
                            if(serviceNameOverrides.containsKey(serviceKey)) {

                                isCertValid = certificateService.isCertificateValid(serviceNameOverrides.get(serviceKey) as String, 30124)
                            } else {
                                isCertValid = certificateService.isCertificateValid(certificateService.getMicroserviceUrl(REPOSITORY as String, ENVIRONMENT as String))
                            }

                            if (!isCertValid) {
                                throw new RuntimeException("Certificate expirred for ${REPOSITORY}, certs were not rotated")
                            }
                        }
                    })
                }
            }
        }
    }
    post {
        always {
            script {
                String appName = "${REPOSITORY.split("-")[1].capitalize()} ${REPOSITORY.split("-")[2].capitalize()}"
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    if (VERSION != null && VERSION != '') {
                        if(ENVIRONMENT == 'prod') {
                            return
                        }
                        discordSend description: "Jenkins Push to ${ENVIRONMENT.toLowerCase().capitalize()} for ${appName} with version ${VERSION}: ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Kubernetes Deploy", webhookURL: webhookUrl
                    } else if(currentBuild.currentResult != 'SUCCESS' && ENVIRONMENT.toLowerCase() == 'develop') {
                        discordSend description: "Jenkins Certificate Rotation to ${ENVIRONMENT.toLowerCase().capitalize()} for ${appName}: ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Certificate Rotation", webhookURL: webhookUrl
                    }
                }
            }
        }

    }
}
