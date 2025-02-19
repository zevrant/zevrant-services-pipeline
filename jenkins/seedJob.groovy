@Library('CommonUtils') _

import com.zevrant.services.pojo.*;

pipeline {
    agent {
        label 'master-node'
    }
    stages {

        stage("Process Seed File") {
            steps {
                script {
                    sh 'ls -l'
                    print(TerraformCodeUnitCollection.codeUnits.size())

                    jobDsl(
                            targets: 'jenkins/seed.groovy',
                            removedJobAction: 'DELETE',
                            removedViewAction: 'DELETE',
                            removedConfigFilesAction: 'DELETE',
                            lookupStrategy: 'SEED_JOB',
                            failOnMissingPlugin: true,
                            additionalClasspath: 'jenkins/src', //only works with
                    )
                }
            }
        }

        stage ('Update Version Database Schema') {
            agent {
                label 'container-builder'
            }
            environment {
                POSTGRES_USERNAME = 'jenkins'
                POSTGRES_PASSWORD = credentials('jenkins-app-version-password')
                POSTGRES_URL = '10.1.0.18'
                POSTGRES_PORT = '5432'
            }
            steps {
                script {
                    sh './gradlew liquibase update'
                }
            }
        }
    }
}
