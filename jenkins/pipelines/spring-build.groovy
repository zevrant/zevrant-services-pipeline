@Library("CommonUtils") _


import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

List<String> angularProjects = ["zevrant-home-ui"];

BRANCH_NAME = BRANCH_NAME.tokenize("/")
BRANCH_NAME = BRANCH_NAME[BRANCH_NAME.size() - 1];
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
String env = (BRANCH_NAME == "master") ? "prod" : "develop"
Version version = null
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }
    environment {
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        AWS_DEFAULT_REGION = "us-east-1"
        DOCKER_TOKEN = credentials('jenkins-dockerhub')
    }
    stages {
        stage("SCM Checkout") {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        git credentialsId: 'jenkins-git', branch: BRANCH_NAME,
                                url: "git@github.com:zevrant/${REPOSITORY}.git"
                    }
                }
            }
        }

        stage("Javascript Test") {
            when { expression { fileExists('package.json') } }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        sh "npm run test"
                    }
                }
            }

        }

        stage("Java Test") {
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
            steps {
                container('spring-jenkins-slave') {
                    script {
                        versionTasks.minorVersionUpdate(REPOSITORY, version)
                    }
                }
            }
        }

        stage("Release Version Update") {
            when { expression { BRANCH_NAME == "master" } }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        versionTasks.majorVersionUpdate(REPOSITORY, version)
                    }
                }
            }
        }

        stage("Build Artifact") {
            steps {

                script {
                    container('spring-jenkins-slave') {
                        sh "bash gradlew clean assemble"
                    }
                    container('buildah') {
                        sh 'echo $DOCKER_TOKEN | buildah login -u zevrant --password-stdin docker.io'
                        sh "buildah bud --storage-driver=vfs -t docker.io/zevrant/$REPOSITORY:${version.toThreeStageVersionString()} ."
                        sh "buildah push docker.io/zevrant/$REPOSITORY:${version.toThreeStageVersionString()}"
                    }
                }
            }
        }

        stage("Promote Artifact") {
            when { expression { BRANCH_NAME == "master" } }
            steps {
                script {
                    if (env == "prod") {
                        container('buildah') {
                            sh 'echo $DOCKER_TOKEN | buildah login -u zevrant --password-stdin docker.io'
                            sh "buildah tag docker.io/zevrant/${REPOSITORY}:${version.toThreeStageVersionString()} docker.io/zevrant/${REPOSITORY}:${newVersion.toThreeStageVersionString()}"
                            sh "buildah push docker.io/zevrant/${REPOSITORY}:${newVersion}"
                        }
                    }
                }
            }
        }

        stage("Trigger Deploy") {
            steps {
                script {
                    build job: "${REPOSITORY}-deploy-to-${env}", parameters: [
                            [$class: 'StringParameterValue', name: 'VERSION', value: version.toThreeStageVersionString()],
                            [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
                    ],
                    wait: false
                }
            }
        }
    }
}