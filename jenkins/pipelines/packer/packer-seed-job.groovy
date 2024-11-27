import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection

@Library('CommonUtils') _



pipeline {
    agent {
        label 'master-node'
    }

    steps {
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
                                additionalParameters: [
                                        images: PackerCodeUnitCollection.packerImages,
                                ]
                        )
                    }
                }
            }
        }
    }
}