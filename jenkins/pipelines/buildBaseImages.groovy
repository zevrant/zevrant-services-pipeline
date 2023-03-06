import java.util.List

def imagesToBuild = ["ubuntu-base", 'android-emulator']
def branchesToBuild = ["develop"]
Map<String, List<String>> affectedRepos = new HashMap<>();
branchesToBuild.each({ branch ->
    affectedRepos.put(branch, []);
})
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
                    git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
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
                            sh 'echo $DOCKER_TOKEN | buildah login -u zevrant --password-stdin docker.io'
                            def imageBuilds = [:]
                            imagesToBuild.each { image ->
                                imageBuilds[image] = {
                                    sh "buildah bud -t docker.io/zevrant/${image}:latest -f ${image}.dockerfile --pull ."
                                    sh "buildah push docker.io/zevrant/${image}:latest"
                                }
                            }
                            parallel imageBuilds
                        }
                    }
                }
            }

        }

        stage("Get Potential Repos") {
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