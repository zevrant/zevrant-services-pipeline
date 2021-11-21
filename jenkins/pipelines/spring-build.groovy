@Library("CommonUtils") _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
Version version = null
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage("Test") {
            when { expression { false } } //TODO fix tests for all services
            steps {
                container('spring-jenkins-slave') {
                    script {
                        sh "bash gradlew clean build"
                    }
                }
            }

        }

        stage("Get Version") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        version = versionTasks.getVersion(REPOSITORY as String) as Version
                        versionCode = versionTasks.getVersionCode("${REPOSITORY.toLowerCase()}")
                        currentBuild.displayName = "Building version ${version.toVersionCodeString()}"
                    }
                }
            }
        }

        stage("Version Update") {
            when { expression { BRANCH_NAME == "master" } }
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                            version = versionTasks.incrementVersion(REPOSITORY, version);
                            currentBuild.displayName = "Building version ${version.toVersionCodeString()}"
                    }
                }
            }
        }

        stage("Build Artifact") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
                DOCKER_TOKEN = credentials('jenkins-dockerhub')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        sh "CI=ci bash gradlew clean assemble"
                    }
                    container('buildah') {
                        String versionString = (BRANCH_NAME == "master")
                                ? version.toVersionCodeString()
                                : "${versionString}-${BRANCH_NAME}" as String
                        sh 'echo $DOCKER_TOKEN | buildah login -u zevrant --password-stdin docker.io'
                        sh "buildah bud -t docker.io/zevrant/$REPOSITORY:${versionString} ."
                        sh "buildah push docker.io/zevrant/$REPOSITORY:${versionString}"
                    }
                }
            }
        }

        stage("Trigger Deploy") {
            when { expression { BRANCH_NAME == "master" } }
            steps {
                script {
                    String[] repositorySplit = REPOSITORY.split("-")
                    String versionString = (BRANCH_NAME == master)
                            ? version.toVersionCodeString()
                            : "${versionString}-${BRANCH_NAME}" as String
                    build job: "Spring/${repositorySplit[0].capitalize()} ${repositorySplit[1].capitalize()} ${repositorySplit[2].capitalize()}/${REPOSITORY}-deploy-to-develop" as String, parameters: [
                            [$class: 'StringParameterValue', name: 'VERSION', value: versionString],
                    ],
                            wait: false
                }
            }
        }
    }
}