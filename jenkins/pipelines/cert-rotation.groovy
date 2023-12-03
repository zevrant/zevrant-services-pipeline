@Library("CommonUtils") _


import com.zevrant.services.pojo.CertRotationInfo
import com.zevrant.services.pojo.KubernetesEnvironment
import com.zevrant.services.pojo.KubernetesService
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.ThanosQueryService

CertificateService certificateService = new CertificateService(this)
ThanosQueryService thanosQueryService = new ThanosQueryService(this)
List<CertRotationInfo> certsToRotate = []

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
//        stage('Determine Expiring Certificates') {
//            steps {
//                script {
//                    certsToRotate = thanosQueryService.getServicesNeedingCertRotation()
//                    println "There are ${certsToRotate.size()} to rotate!!"
//                }
//            }
//        }
        stage('Get Expired Services') {
            steps {
                script {
                    KubernetesServiceCollection.services
                            .findAll({ service -> service.url != null && service.url != '' })
                            .each { service ->
                                if (!certificateService.isCertificateValid(service.url)) {
                                    certsToRotate.add(new CertRotationInfo(service.name, null, null, service.getEnvironments().get(0).namespaceName))
                                }
                            }
                    SpringCodeUnitCollection.microservices
                            .findAll({service -> service.enabled})
                            .each { service ->
                                if (!certificateService.isCertificateValid("${service.deploymentName}.${KubernetesEnvironment.DEVELOP.getNamespaceName()}.svc.cluster.local")) {
                                    certsToRotate.add(new CertRotationInfo(service.name, null, null, KubernetesEnvironment.DEVELOP.getNamespaceName()))
                                }

//                                if (!certificateService.isCertificateValid("${service.deploymentName}.${KubernetesEnvironment.PROD.getNamespaceName()}.svc.cluster.local")) {
//                                    certsToRotate.add(new CertRotationInfo(service.name, null, null, KubernetesEnvironment.PROD.getNamespaceName()))
//                                }
                            }
                }
            }
        }

        stage('Trigger Application Cert Rotation') {
            steps {
                script {
                    certsToRotate.each({ cert ->
                        String name = cert.secretName
                                .replace('-tls', '')
                                .replace('-internal', '')
                                .split('-')
                                .toUnique()
                                .join('-')

                        println "Rotating ${name}"
                        String serviceName = ''
                        String folder = ''
                        try {
                            serviceName = KubernetesServiceCollection.findServiceByName(name).name
                            folder = 'kubernetes-services'
                        } catch (Exception ignored) {
                            serviceName = SpringCodeUnitCollection.findServiceByServiceName(name).name
                            serviceName = "${serviceName}/${serviceName}-deploy-to-develop"
                            folder = 'Spring'
                        }
                        try {
                            build job: "${folder}/${serviceName}", wait: false
                        } catch (Exception ex) {
                            println("Failed to rotate service $name")
                            withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                                discordSend description: "Jenkins failed to rotate certs for ${serviceName}", result: currentBuild.currentResult, title: "Certificate Rotation", webhookURL: webhookUrl
                            }
                        }
                    })
                }
            }
        }
    }
}