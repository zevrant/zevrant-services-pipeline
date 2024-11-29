package packer

@Library('CommonUtils') _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.services.GitService


pipeline {
    agent {
        label 'master-node'
    }

    stages {
        stage('Retrieve Job Dsl') {
            steps {
                script {
                    GitService gitService = new GitService(this)

                    gitService.checkout('git@github.com', 'zevrant', 'zevrant-services-pipeline',
                            'master', 'jenkins-git')
                }
            }
        }

        stage('Update Job Configurations') {
            steps {
                script {
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