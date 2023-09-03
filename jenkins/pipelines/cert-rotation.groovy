@Library("CommonUtils") _

import com.zevrant.services.services.ThanosQueryService

ThanosQueryService thanosQueryService = new ThanosQueryService(this)

pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }

    stages {
        stage('Determine Expiring Certificates') {
            steps {
                script {
                    thanosQueryService.getServicesNeedingCertRotation()
                }
            }
        }

        stage('Trigger Application Cert Rotation') {
            steps {
                script {
                    println 'placeholder!!'
                }
            }
        }
    }
}