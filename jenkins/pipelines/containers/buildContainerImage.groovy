@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService
import com.zevrant.services.pojo.containers.Image

ImageBuildService imageBuildService = new ImageBuildService(this)

GitService gitService = new GitService(this)
Image image = null
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('SCM Checkout') {
            steps {
                script {
                    gitService.checkout('containers')
                }
            }
        }

        stage ('Registry Login & Setup') {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                script {
                    def imageConfig = pipelineContext.readJSON(file: 'buildConfig.json' as String)
                    image = imageBuildService.parseImageConfig(imageConfig as LinkedHashMap<String, Object>, pwd() as String)
                    imageBuildService.registryLogin(DOCKER_CREDENTIALS_USR, DOCKER_CREDENTIALS_PSW, 'harbor.zevrant-services.internal')
                }
            }
        }

        stage ("Build ${IMAGE_NAME}"){
            steps {
                script {
                    dir(BUILD_DIR_PATH) {

                        imageBuildService.buildImage(image)
                    }
                }
            }
        }

        stage ('Push Image') {
            steps {
                script {
                    imageBuildService.pushImage(image)
                }
            }
        }
    }
}