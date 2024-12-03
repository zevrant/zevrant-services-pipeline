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
            when { changeset 'src/main/resources/liquibase/liquibase-changelog.yml'}
            agent {
                label 'container-builder'
            }
            environment {
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGHOST = '192.168.0.101'
            }
            steps {
                script {
                    sh './gradlew liquibase update'
                }
            }
        }
    }
}
