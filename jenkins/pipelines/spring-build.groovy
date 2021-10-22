@Library("CommonUtils") _


import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

List<String> angularProjects = ["zevrant-home-ui"];

BRANCH_NAME = BRANCH_NAME.tokenize("/")
BRANCH_NAME = BRANCH_NAME[BRANCH_NAME.size() - 1];
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
Version version = null;
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
    }
    stages {
        stage("Get Version") {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        version = versionTasks.getVersion(REPOSITORY as String)
                        versionCode = versionTasks.getVersionCode("${REPOSITORY.toLowerCase()}")
                    }
                }
            }
        }

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
                        sh "buildah bud --storage-driver=vfs -t zevrant/$REPOSITORY:${version.toThreeStageVersionString()} ."
                        sh "buildah push zevrant/$REPOSITORY:${version.toThreeStageVersionString()}"
                    }
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    String env = (BRANCH_NAME == "master") ? "prod" : "develop"
                    if (env == "prod") {
                        container('buildah') {
                            sh "buildah tag docker.io/zevrant/${REPOSITORY}:${version.toThreeStageVersionString()} zevrant/${REPOSITORY}:${newVersion.toThreeStageVersionString()}"
                            sh "buildah push docker.io/zevrant/${REPOSITORY}:${newVersion}"
                        }
                    }
                    build job: 'Deploy', parameters: [
                            [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
                            [$class: 'StringParameterValue', name: 'VERSION', value: version.toThreeStageVersionString()],
                            [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
                    ]
                }
            }
        }
    }
}