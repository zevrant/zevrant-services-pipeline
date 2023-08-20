@Library('CommonUtils') _

pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
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
                            additionalClasspath: '**/*.groovy', //only works with
                    )
                }
            }
        }
    }
}
