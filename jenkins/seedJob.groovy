@Library('CommonUtils@master') _

import com.zevrant.services.pojo.TerraformCodeUnitCollection
import com.zevrant.services.pojo.CodeUnit
pipeline {
    agent {
        label 'master-node'
    }
    stages {

        stage("Process Seed File") {
            steps {
                script {
                    sh 'ls -l'
                    List<CodeUnit> codeUnits = TerraformCodeUnitCollection.getCodeUnits()
                    print(codeUnits.size())

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
