@Library("CommonUtils")

import groovy.json.JsonSlurper
import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionTasks

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
Version version;
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }
    stages {

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
                        version = versionTasks.getVersion(REPOSITORY as String) as Version
                        currentBuild.displayName = "Building Version ${version.toVersionCodeString()}" as String
                    }
                }
            }
        }

        stage("Version Update") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        versionTasks.incrementVersion(REPOSITORY, version)

                    }
                }
            }
        }

        stage("Build & Publish") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        sh "bash gradlew clean assemble publish -PprojVersion=${version.toVersionCodeString()} --no-daemon"
                    }
                }
            }
        }
    }
}