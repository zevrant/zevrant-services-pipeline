import com.zevrant.services.ServiceLoader
import com.zevrant.services.services.GitService

import java.util.List

//def imagesToBuild = ["ubuntu-base", 'android-emulator']
//def branchesToBuild = ["develop"]
Map<String, List<String>> affectedRepos = new HashMap<>();
//branchesToBuild.each({ branch ->
//    affectedRepos.put(branch, []);
//})
GitService gitService = ServiceLoader.load(binding, GitService.class) as GitService

List<String> baseImageFolders = []

pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage("Git Checkout") {
            steps {
                script {
                    gitService.checkout('zevrant-services-pipeline')
                }
            }
        }

        stage ("Find Base Images") {
            steps {
                script {
                    dir("docker/dockerfile") {
                        sh 'ls -l > directory-contents'
                        String directoryContents = readFile(file: 'directory-contents')
                        directoryContents.split('\n').each({ line ->
                            List<String> lineContents = line.split("\\h")
                            if(lineContents[0].contains('d')) {
                                dir(lineContents[lineContents.size() - 1]) {
                                    if(fileExists(file: 'buildConfig.json')) {
                                        def buildInfo = readJSON(file: 'buildConfig.json')
                                        if (buildInfo.baseImage != null) {
                                            baseImageFolders.add(lineContents[lineContents.size() - 1])
                                        }
                                    }
                                }

                            }
                        })

                    }
                }
            }
        }

        stage("Build & Push Dockerfiles") {
            environment {
                DOCKER_TOKEN = credentials('jenkins-dockerhub')
            }
            steps {
                script {
                    container('buildah') {
                        dir("docker/dockerfile") {
                            sh 'echo $DOCKER_TOKEN | buildah login -u \'robot$jenkins\' --password-stdin harbor.zevrant-services.com'
//                            def imageBuilds = [:]
//                            imagesToBuild.each { image ->
//                                imageBuilds[image] = {
                            baseImageFolders.each {folder ->
                                dir(folder) {
                                    if(fileExists(file: 'buildConfig.json')) {
                                        def buildInfo = readJSON(file: 'buildConfig.json')
                                        buildInfo.baseImage.each { key, value ->
                                            println "Contents of base image object for image ${buildInfo.name}"
                                            println "Key: ${key}, Value: ${value}"
                                        }
                                        if (buildInfo.baseImage['repository'] == null || buildInfo.baseImage['repository'] == "") {
                                            sh "buildah pull ${buildInfo.baseImage.host}/${buildInfo.baseImage.name}:${buildInfo.baseImage.tag}"
                                        } else {
                                            sh "buildah pull ${buildInfo.baseImage.host}/${buildInfo.baseImage.repository}/${buildInfo.baseImage.name}:${buildInfo.baseImage.tag}"
                                        }

                                        String tag = (buildInfo.useLatest) ? "latest" : buildInfo.version

                                        sh "buildah bud -t harbor.zevrant-services.com/zevrant-services/${buildInfo.name}:${tag} ."
                                        sh "buildah push harbor.zevrant-services.com/zevrant-services/${buildInfo.name}:${tag}"
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        stage("Get Potential Repos") {
            when{ expression { false } }
            steps {
                script {
                    def response = httpRequest authentication: 'jenkins-git-access-token', url: "https://api.github.com/orgs/zevrant/repos?type=all"
                    List jsonResponse = readJSON text: response.content
                    def parralelSteps = [:]
                    jsonResponse.each { repo ->
                        if ((repo['name'] as String).contains('zevrant')
                                && repo['name'] as String != 'zevrant-services-pipeline'
                                && !(repo['archived'] as Boolean)) {
                            branchesToBuild.each({ branch ->
                                parralelSteps["${repo['name']}:$branch"] = {
                                    def dockerfileResponse = httpRequest(authentication: 'jenkins-git-access-token',
                                            contentType: "TEXT_PLAIN",
                                            validResponseCodes: "200:404",
                                            url: "https://raw.githubusercontent.com/zevrant/${repo['name'] as String}/master/Dockerfile")
                                    if (dockerfileResponse.status < 400) {
                                        for (image in imagesToBuild) {
                                            if (dockerfileResponse.content.contains(image)) {
                                                affectedRepos.get(branch).add(repo['name'] as String);
                                                break;
                                            }
                                        }
                                    }
                                }
                            })


                        }
                    }
                    parallel parralelSteps
                }
            }
        }

        stage("Rebuild Downstream Repos") {
            when{ expression { false } }
            steps {
                script {
                    def buildJobs = [:];
                    branchesToBuild.each { branch ->
                        println("Build $branch branch for these repositories")
                        affectedRepos.get(branch).each { repo ->
                            String[] repoBits = repo.split("-")
                            String jenkinsAppName = "${repoBits[0].capitalize()} ${repoBits[1].capitalize()} ${repoBits[2].capitalize()}"
                            buildJobs["Build $branch for $repo"] = {
                                build job: "Spring/${jenkinsAppName}/${repo}-multibranch/master", parameters: [
                                        [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: "refs/heads/master"]
                                ]
                            }
                        }
                    }
                    parallel buildJobs
                }
            }
        }
    }
}