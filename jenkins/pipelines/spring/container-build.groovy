@Library('CommonUtils') _


import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService

ImageBuildService imageBuildService = new ImageBuildService(this)

GitService gitService = new GitService(this)
SpringCodeUnit springCodeUnit = SpringCodeUnitCollection.findByRepoName(repository)
Image image = new Image(springCodeUnit.name, "", true, null, 'harbor.zevrant-services.internal', 'zevrant-services', '')
Version version = null
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Get Artifacts') {
            steps {
                script {
                    copyArtifacts filter: 'artifactVersion.txt', fingerprintArtifacts: true, projectName: "./${springCodeUnit.name}-multibranch/main"
                    String versionString = readFile(file: 'artifactVersion.txt')
                    version = new Version(versionString)
                    image.setVersion(versionString)
                    String dockerfile = httpRequest(
                            authentication: 'gitea-access-token',
                            url: "https://gitea.zevrant-services.internal/zevrant-services/containers/raw/branch/main/k8s/spring-microservice-template/Dockerfile"
                    ).content
                    String baseImage = ((String[]) dockerfile.split("\n"))[0].split(" ")[1]
                    sh 'rm -f Dockerfile'
                    writeFile(file: 'Dockerfile', text: dockerfile)
                    httpRequest(
                            authentication: 'gitea-access-token',
                            url: "https://gitea.zevrant-services.com/zevrant-services/-/packages/maven/com.zevrant.services-oauth2-service/0.0.1-snapshot/files/30",
                            outputFile: "${springCodeUnit.name}-${version.toVersionCodeString()}.jar"
                    )
                }
            }
        }

        stage('Registry Login') {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                script {
                    imageBuildService.registryLogin(DOCKER_CREDENTIALS_USR, DOCKER_CREDENTIALS_PSW, 'harbor.zevrant-services.internal')
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