@Library('CommonUtils') _


pipeline {
    agent {
        label 'master-node'
    }
    stages {

        stage("Process Seed File") {
            steps {
                script {
                    sh 'ls -l'
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
                POSTGRES_URL = '192.168.0.101'
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
