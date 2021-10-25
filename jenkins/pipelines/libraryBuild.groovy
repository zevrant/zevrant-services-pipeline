@Library("CommonUtils")

import groovy.json.JsonSlurper
import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

String repository = env.JOB_BASE_NAME
Version version;
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
BRANCH_NAME = BRANCH_NAME.tokenize("/")
BRANCH_NAME = BRANCH_NAME[BRANCH_NAME.size() - 1];
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
                        git credentialsId: 'jenkins-git', branch: env.BRANCH_NAME,
                                url: "git@github.com:zevrant/${repository}.git"
                    }
                }
            }
        }

        stage("Test") {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        "bash gradlew clean build --no-daemon"
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
                        versionTasks.minorVersionUpdate(REPOSITORY, version)
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
                        versionTasks.majorVersionUpdate(REPOSITORY, version)
                    }
                }
            }
        }

        stage("Build & Publish") {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        sh "bash gradlew clean assemble publish -PprojVersion=${version} --no-daemon"
                    }
                }
            }
        }
    }
}