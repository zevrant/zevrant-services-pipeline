@Library('CommonUtils') _


import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService
import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.services.NotificationService

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
                    retry 3, {
                        try {
                            gitService.checkout('containers')
                        } catch (Exception rethrown) {
                            sleep 5
                            throw rethrown
                        }
                    }
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
                String imageName = (image != null)? image.name : ''
                new NotificationService(this).sendDiscordNotification(
                        "Jenkins Failed to Build Container Image ${imageName}",
                        env.BUILD_URL,
                        currentBuild.currentResult,
                        "Jenkins Image Build - ${imageName}",
                        NotificationChannel.DISCORD_CICD
                )
            }
        }
    }
//                if (image != null) {
//                    String taglessImage = "${image.host}/${image.repository}/${image.name}".replace('//', '/')
//                    println taglessImage
//                    sh 'buildah images --noheading'
//                    sh "buildah images --noheading | awk '{ print \$3 }'"
//                    sh "buildah images --noheading | grep ${taglessImage} | awk '{ print \$3 }' | tee imageToRemove"
//                    String containerIds = readFile('imageToRemove')
//                    containerIds.split('\\n').each { id ->
//                        sh "buildah rmi ${id}"
//                    }
//
//                }
//            }
//        }
//    }
}