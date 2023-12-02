@Library("CommonUtils") _


import com.zevrant.services.pojo.CertRotationInfo
import com.zevrant.services.pojo.KubernetesService
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.services.CertificateService
import com.zevrant.services.services.ThanosQueryService

CertificateService certificateService = new CertificateService(this)
ThanosQueryService thanosQueryService = new ThanosQueryService(this)
List<CertRotationInfo> certsToRotate = Collections.emptyList()

pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
        stage('Determine Expiring Certificates') {
            steps {
                script {
                    certsToRotate = thanosQueryService.getServicesNeedingCertRotation()
                    println "There are ${certsToRotate.size()} to rotate!!"
                }
            }
        }
        stage('Get Expired Services') {
            steps {
                script {
                    KubernetesServiceCollection.services
                            .findAll({ service -> service.url != null && service.url != '' })
                            .each { service ->
                                if (!certificateService.isCertificateValid(service.url)) {
                                    certsToRotate.add(new CertRotationInfo(service.name, null, null))
                                }
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
                            folder = 'Spring'
                        }
                        try {
                            retry(3, {
                                build job: "${folder}/${serviceName}", wait: true
                            })
                        } catch (Exception ex) {
                            println("Failed to rotate service $name")
                        }
                    })
                }
            }
        }
    }
}