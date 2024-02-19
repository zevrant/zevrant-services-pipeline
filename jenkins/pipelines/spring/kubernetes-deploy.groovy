package spring

import com.zevrant.services.ServiceLoader
import com.zevrant.services.pojo.KubernetesEnvironment
import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.GitService
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.PostgresYamlConfigurer
import com.zevrant.services.services.VersionService

import java.nio.charset.StandardCharsets

CertificateService certificateService = new CertificateService(this)
KubernetesService kubernetesService = new KubernetesService(this)
GitService gitService = new GitService(this)
LinkedHashMap<String, Serializable> serviceNameOverrides = [
        'develop-zevrant-home-ui'     : '172.16.1.10',
        'develop-zevrant-home-ui-port': 30124
] as LinkedHashMap<String, Serializable>

SpringCodeUnit codeUnit = SpringCodeUnitCollection.microservices.find { unit -> unit.repo.repoName == REPOSITORY }
VersionService versionService = new VersionService(this)
if (codeUnit == null) {
    throw new RuntimeException("Failed to find Spring Code Unit for repository $REPOSITORY")
}

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
                        git credentialsId: 'jenkins-git', branch: 'main',
                                url: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/${REPOSITORY}.git" as String
                    }
                }
            }
        }

        stage("Deploy Micro Service") {
            when { expression {  VERSION != null && VERSION != '' } }
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                container('kubectl') {
                    script {
                        sh 'echo $DOCKER_CREDENTIALS_PSW | helm registry login harbor.zevrant-services.internal --username $DOCKER_CREDENTIALS_USR --password-stdin'
                        try {
                            sh "helm upgrade --install --namespace ${environment} ${codeUnit.name} oci://harbor.zevrant-services.internal/zevrant-services/${codeUnit.name} --version ${VERSION} -f ${ENVIRONMENT}-values.yml --wait"
                                                            //helm pull oci://harbor.zevrant-services.internal/zevrant-services/oauth2-service --version 0.0.1
                        } catch (Exception ignored) {
                            sh "helm rollback ${codeUnit.name} --namespace ${environment}"
                            throw ignored
                        }
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
                            Version version = version = versionService.getVersion(REPOSITORY as String) as Version
                            println("Redeploying version ${version.toThreeStageVersionString()}")
                            currentBuild.displayName = "Rotating Certificates for Version ${version.toThreeStageVersionString()}"
                            try {
                                sh "kubectl get deployment ${codeUnit.getDeploymentName()} -n $ENVIRONMENT"
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to retrieve deployment for repository $REPOSITORY in $ENVIRONMENT")
                            }
                            sh "kubectl rollout restart deployments ${codeUnit.getDeploymentName()} -n $ENVIRONMENT"
                            int timeout = kubernetesService.getDeployTimeout(ENVIRONMENT == 'prod' ? 2 : 1)
//                            try {
                            sh "kubectl rollout status deployments ${codeUnit.getDeploymentName()} -n $ENVIRONMENT --timeout=${timeout}s"
//                            } catch (Exception ignored) {
//                                sh "kubectl rollout undo deploy $REPOSITORY -n $ENVIRONMENT"
//                                throw new RuntimeException("Deployment for $REPOSITORY in Environment $ENVIRONMENT failed and was rolled back")
//                            }
                            boolean isCertValid = false

                            isCertValid = certificateService.isCertificateValid("${codeUnit.deploymentName}.${ENVIRONMENT}.svc.cluster.local", 443)

                            if (!isCertValid) {
                                throw new RuntimeException("Certificate expired for ${REPOSITORY}, certs were not rotated")
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
                String appName = "${REPOSITORY.split('-').collect { part -> part.capitalize() }.join(' ')}"
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    if (VERSION != null && VERSION != '') {
                        if (ENVIRONMENT == 'prod') {
                            return
                        }
                        discordSend description: "Jenkins Push to ${ENVIRONMENT.toLowerCase().capitalize()} for ${appName} with version ${VERSION}: ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Kubernetes Deploy", webhookURL: webhookUrl
                    } else if (currentBuild.currentResult != 'SUCCESS' && ENVIRONMENT.toLowerCase() == 'develop') {
                        discordSend description: "Jenkins Certificate Rotation to ${ENVIRONMENT.toLowerCase().capitalize()} for ${appName}: ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Certificate Rotation", webhookURL: webhookUrl
                    }
                }
            }
        }

    }
}
