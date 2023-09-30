@Library("CommonUtils") _


import com.zevrant.services.pojo.CertRotationInfo
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.services.ThanosQueryService

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

        stage('Trigger Application Cert Rotation') {
            steps {
                script {
                    certsToRotate.each({cert ->
                        String serviceName = cert.secretName
                                .replace('-tls', '')
                                .replace('-internal', '')
                        println "Rotating ${serviceName}"
                        if(serviceName.contains('jenkins')) {
                            build job: "A"
                        } else if(KubernetesServiceCollection.findServiceByName(serviceName) != null) {
                            build job: "kubernetes-services/${serviceName}", wait: true
                        }
                    })
                }
            }
        }
    }
}