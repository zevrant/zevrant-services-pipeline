@Library('CommonUtils') _

import com.zevrant.services.services.GitService


GitService gitService = new GitService(this)

pipeline {
    agent {
        label 'master-node'
    }

    stages {
        stage('Retrieve Job Dsl') {
            steps {
                script {
                    gitService.checkout('git@github.com', 'zevrant', 'zevrant-services-pipeline',
                            'master', 'jenkins-git')
                }
            }
        }

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