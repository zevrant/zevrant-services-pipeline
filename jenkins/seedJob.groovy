@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService
import com.zevrant.services.pojo.containers.Image
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper



GitService gitService = new GitService(this)
ImageBuildService imageBuildService = new ImageBuildService(this)
List<Image> images = []
pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }
    stages {

        stage("Search Images") {
            steps {
                script {
                    dir ('containers') {
                        gitService.checkout('containers')
                        List<FileWrapper> files = findFiles(glob: '*/*/buildConfig.json')
                        images = imageBuildService.parseAvailableImages(files, 'harbor.zevrant-services.internal', 'zevrant-services')
                        println images.size()
                    }
                }
            }
        }

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
                            additionalParameters: [
                                    images     : new ArrayList<>(images),
                            ]
                    )
                }
            }
        }
    }
}
