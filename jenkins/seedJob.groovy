@Library('CommonUtils') _


import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService

GitService gitService = new GitService(this)
ImageBuildService imageBuildService = new ImageBuildService(this)
List<Image> images = []
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
                            additionalClasspath: 'jenkins/src/main/groovy', //only works with
                    )
                }
            }
        }
    }
}
