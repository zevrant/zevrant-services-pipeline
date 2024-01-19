import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionService

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME

Version version;
VersionService versionTasks = new VersionService(this)
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
                REDISCLI_AUTH = credentials('jenkins-keydb-password')
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
                REDISCLI_AUTH = credentials('jenkins-keydb-password')
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
    post {
        always {
            script {
                String appName = "${REPOSITORY.split("-")[1].capitalize()} ${REPOSITORY.split("-")[2].capitalize()}"
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Build for ${appName} on branch ${branchName} building version ${version.toVersionCodeString()} was ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Spring Build", webhookURL: webhookUrl
                }
            }
        }
    }
}
