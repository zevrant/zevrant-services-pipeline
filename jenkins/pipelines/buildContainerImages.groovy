@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.services.ImageBuildService
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import java.awt.Image

GitService gitService = new GitService(this)
ImageBuildService imageBuildService = new ImageBuildService(this)
List<Image> images = null

pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage("Git Checkout") {
            steps {
                script {
                    gitService.checkout('containers')
                }
            }
        }

        stage("Build Base Images") {
            steps {
                script {
                    List<FileWrapper> files = findFiles(glob: '*/*/buildConfig.json')
                    images = imageBuildService.parseAvailableImages(files, 'harbor.zevrant-services.com', 'zevrant-services')
                }
            }
        }

        stage("Build & Push Dockerfiles") {
            environment {
                DOCKER_CREDENTIALS = credentials('jenkins-harbor')
            }
            steps {
                script {
                    imageBuildService.registryLogin(DOCKER_CREDENTIALS_USR, DOCKER_CREDENTIALS_PSW, 'harbor.zevrant-services.com')
                    imageBuildService.buildImagesInParallel(images, 'harbor.zevrant-services.com')
                }
            }

        }

//        stage("Get Potential Repos") {
//            steps {
//                script {
//                    def response = httpRequest authentication: 'jenkins-git-access-token', url: "https://gitea.zevrant-services.internal/api/v1/orgs/zevrant/repos?type=all"
//                    List jsonResponse = readJSON text: response.content
//                    def parralelSteps = [:]
//                    jsonResponse.each { repo ->
//                        if ((repo['name'] as String).contains('zevrant')
//                                && repo['name'] as String != 'zevrant-services-pipeline'
//                                && !(repo['archived'] as Boolean)) {
//                            branchesToBuild.each({ branch ->
//                                parralelSteps["${repo['name']}:$branch"] = {
//                                    def dockerfileResponse = httpRequest(authentication: 'jenkins-git-access-token',
//                                            contentType: "TEXT_PLAIN",
//                                            validResponseCodes: "200:404",
//                                            url: "https://raw.githubusercontent.com/zevrant/${repo['name'] as String}/main/Dockerfile")
//                                    if (dockerfileResponse.status < 400) {
//                                        for (image in imagesToBuild) {
//                                            if (dockerfileResponse.content.contains(image)) {
//                                                affectedRepos.get(branch).add(repo['name'] as String);
//                                                break;
//                                            }
//                                        }
//                                    }
//                                }
//                            })
//
//
//                        }
//                    }
//                    parallel parralelSteps
//                }
//            }
//        }
//
//        stage("Rebuild Downstream Repos") {
//            steps {
//                script {
//                    def buildJobs = [:];
//                    branchesToBuild.each { branch ->
//                        println("Build $branch branch for these repositories")
//                        affectedRepos.get(branch).each { repo ->
//                            String[] repoBits = repo.split("-")
//                            String jenkinsAppName = "${repoBits[0].capitalize()} ${repoBits[1].capitalize()} ${repoBits[2].capitalize()}"
//                            buildJobs["Build $branch for $repo"] = {
//                                build job: "Spring/${jenkinsAppName}/${repo}-multibranch/main", parameters: [
//                                        [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: "refs/heads/main"]
//                                ]
//                            }
//                        }
//                    }
//                    parallel buildJobs
//                }
//            }
//        }
    }
    post {
        always {
            sh 'buildah rm --all'
        }
    }
}
