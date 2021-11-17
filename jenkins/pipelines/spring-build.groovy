@Library("CommonUtils") _


import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
branchName = branchName.tokenize("/")
branchName = branchName[branchName.size() - 1];
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
String env = (BRANCH_NAME == "master") ? "prod" : "develop"
Version version = null
Version previousVersion = null
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage("SCM Checkout") {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        git credentialsId: 'jenkins-git', branch: branchName,
                                url: "git@github.com:zevrant/${REPOSITORY}.git"
                    }
                }
            }
        }

        stage("Java Test") {
            when { expression { false && env != "prod" } } //TODO fix tests for all services
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
                        version = versionTasks.getVersion(REPOSITORY as String)
                        versionCode = versionTasks.getVersionCode("${REPOSITORY.toLowerCase()}")
                        currentBuild.displayName = "Building version ${version.toThreeStageVersionString()}"
                    }
                }
            }
        }

        stage("Develop Version Update") {
            when { expression { BRANCH_NAME == "develop" } }
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        version = versionTasks.minorVersionUpdate(REPOSITORY, version)
                        currentBuild.displayName = "Building version ${version.toThreeStageVersionString()}"
                    }
                }
            }
        }

        stage("Release Version Update") {
            when { expression { BRANCH_NAME == "master" } }
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        previousVersion = new Version(version.toThreeStageVersionString())
                        versionTasks.majorVersionUpdate(REPOSITORY, version)
                    }
                }
            }
        }

        stage("Build Artifact") {
            when { expression { env != "prod"}}
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
                DOCKER_TOKEN = credentials('jenkins-dockerhub')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        sh "bash gradlew clean assemble"
                    }
                    container('buildah') {
                        if(BRANCH_NAME == "develop") {
                            sh 'echo $DOCKER_TOKEN | buildah login -u zevrant --password-stdin docker.io'
                            sh "buildah bud -t docker.io/zevrant/$REPOSITORY:${version.toThreeStageVersionString()} ."
                            sh "buildah push docker.io/zevrant/$REPOSITORY:${version.toThreeStageVersionString()}"
                        }
                    }
                }
            }
        }

        stage("Trigger Deploy") {
            when { expression { BRANCH_NAME == "develop" || BRANCH_NAME == "master" } }
            steps {
                script {
                    build job: "${REPOSITORY}-deploy-to-${env}", parameters: [
                            [$class: 'StringParameterValue', name: 'VERSION', value: (BRANCH_NAME == "master")? previousVersion.toThreeStageVersionString() : version.toThreeStageVersionString()],
                            [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
                    ],
                            wait: false
                }
            }
        }
    }
}