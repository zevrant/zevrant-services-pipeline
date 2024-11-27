@Library('CommonUtils') _



pipeline {
    agent {
        label 'master-node'
    }

    stages {
        stage('Update Job Configurations') {
            steps {
                script {
                    dir('pipeline') {
                        jobDsl(
                                targets: 'jenkins/pipelines/packer/packer-job-dsl.groovy',
                                removedJobAction: 'DELETE',
                                removedViewAction: 'DELETE',
                                removedConfigFilesAction: 'DELETE',
                                lookupStrategy: 'SEED_JOB',
                                failOnMissingPlugin: true,
                                additionalClasspath: 'jenkins/src/main/groovy', //only works with
                        )
                    }
                }
            }
        }
    }
}