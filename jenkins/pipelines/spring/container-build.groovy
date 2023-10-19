@Library('CommonUtils') _


import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService

ImageBuildService imageBuildService = new ImageBuildService(this)

GitService gitService = new GitService(this)
Image image = new Image()
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Get Template') {
            steps {
                script {
                    String dockerfile = httpRequest(
                            authentication: 'gitea-access-token',
                            url: "https://gitea.zevrant-services.internal/zevrant-services/containers/raw/branch/main/k8s/spring-microservice-template/Dockerfile"
                    ).content
                    String baseImage = ((String[]) dockerfile.split("\n"))[0].split(" ")[1]
                    sh 'echo $DOCKER_TOKEN | buildah login -u \'robot$jenkins\' --password-stdin docker.io'
                    retry(3, {
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "buildah pull $baseImage"
                        }
                    })
                    sh 'rm -f Dockerfile'
                    writeFile(file: 'Dockerfile', text: dockerfile)
                }
            }
        }

        stage('Registry Login') {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                script {
                    dir(BUILD_DIR_PATH) {
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
//    post {
//        always {
//            script {
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