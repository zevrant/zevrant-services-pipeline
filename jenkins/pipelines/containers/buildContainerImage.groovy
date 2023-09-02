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

        stage('Registry Login & Setup') {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                script {
                    dir(BUILD_DIR_PATH) {
                        def imageConfig = readJSON(file: 'buildConfig.json' as String)
                        image = imageBuildService.parseImageConfig(imageConfig, pwd() as String)
                        image.buildDirPath = BUILD_DIR_PATH
                        imageBuildService.registryLogin(DOCKER_CREDENTIALS_USR, DOCKER_CREDENTIALS_PSW, 'harbor.zevrant-services.internal')
                    }
                }
            }
        }

        stage("Build Container") {
            steps {
                script {
                    imageBuildService.buildImage(image)
                }
            }
        }

        stage('Push Image') {
            steps {
                script {
                    imageBuildService.pushImage(image)
                }
            }
        }
    }
    post {
        always {
            script {
                if (image != null) {
                    String taglessImage = "${image.host}/${image.repository}/${image.name}".replace('//', '/')
                    println taglessImage
                    sh 'buildah images --noheading'
                    sh "buildah images --noheading | awk '{ print \$3 }'"
                    sh "buildah images --noheading | grep ${taglessImage} | awk '{ print \$3 }' | tee imageToRemove"
                    String containerIds = readFile('imageToRemove')
                    println containerId
                    containerIds.split('\\n').each { id ->
                        sh "buildah rmi ${containerId}"
                    }

                }
            }
        }
    }
}