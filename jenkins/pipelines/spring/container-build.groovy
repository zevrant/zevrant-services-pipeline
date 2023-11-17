@Library('CommonUtils') _


import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService
import com.zevrant.services.services.VersionService

ImageBuildService imageBuildService = new ImageBuildService(this)
VersionService versionService = new VersionService(this, false)
SpringCodeUnit springCodeUnit = SpringCodeUnitCollection.findByRepoName(repository)
Image image = new Image(
        springCodeUnit.name,
        "",
        true,
        null,
        'harbor.zevrant-services.internal',
        'zevrant-services',
        '',
        ["serviceName=${springCodeUnit.name}"]
)
Version version = null
Version chartVersion = null
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Get Artifacts') {
            steps {
                script {
                    copyArtifacts filter: 'artifactVersion.txt', fingerprintArtifacts: true, projectName: "./${springCodeUnit.name}-multibranch/main"
                    copyArtifacts filter: 'helm-chart.tgz', fingerprintArtifacts: true, projectName: "./${springCodeUnit.name}-multibranch/main"
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
                            url: "https://gitea.zevrant-services.com/api/packages/zevrant-services/maven/com/zevrant/services/${springCodeUnit.name}/${versionString}/${springCodeUnit.name}-${versionString}.jar",
                            outputFile: "${springCodeUnit.name}-${version.toVersionCodeString()}.jar"
                    )
                    sh "jar -t ${springCodeUnit.name}-${version.toVersionCodeString()}.jar"
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

        stage('Package Helm Chart') {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
                REDISCLI_AUTH = credentials('jenkins-keydb-password')
            }
            steps {
                script {
                    untar(file: 'helm-chart.tgz')
                    sh "rm -rf ${springCodeUnit.name}"
                    sh "mv helm ${springCodeUnit.name}"

                    dir(springCodeUnit.name) {
                        sh 'helm dependency build'
                        //Update chart app version with current app version
                        def chartYaml = readYaml(file: 'Chart.yaml')
                        chartVersion = versionService.getVersion("${springCodeUnit.name}-chart")
                        chartYaml.appVersion = version.toVersionCodeString()
                        chartVersion = versionService.minorVersionUpdate("${springCodeUnit.name}-chart", chartVersion)
                        chartYaml.version = chartVersion.toThreeStageVersionString()
                        writeYaml(file: 'Chart.yaml', data: chartYaml, overwrite: true)
                    }
                    sh "helm package ${springCodeUnit.name}"
                    sh 'echo $DOCKER_CREDENTIALS_PSW | helm registry login harbor.zevrant-services.internal --username $DOCKER_CREDENTIALS_USR --password-stdin'
                    sh "helm push ${springCodeUnit.name}-${chartVersion.toThreeStageVersionString()}.tgz oci://harbor.zevrant-services.internal/zevrant-services"
                }
            }
        }
    }
    post {
        success {
            script {
                build job: "./${springCodeUnit.name}-deploy-to-develop", wait: false, parameters: [
                        [$class: 'StringParameterValue', name: 'VERSION', value: chartVersion]
                ]
            }
        }
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
    }
}