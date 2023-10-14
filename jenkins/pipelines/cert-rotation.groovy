@Library("CommonUtils") _


import com.zevrant.services.pojo.CertRotationInfo
import com.zevrant.services.pojo.KubernetesService
import com.zevrant.services.pojo.KubernetesServiceCollection
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

                        println "Rotating ${name}"
                        KubernetesService service = KubernetesServiceCollection.findServiceByName(name)
                        if (service == null) {
                            service = KubernetesServiceCollection.findServiceByServiceName(name)
                        }
                        try {
                            retry(3, {
                                build job: "kubernetes-services/${service.name}", wait: true
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