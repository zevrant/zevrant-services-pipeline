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

        stage("Assemble") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        "bash gradlew clean assemble --info"
                    }
                }
            }
        }

        stage("Test") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        "bash gradlew clean build --info"
                    }
                }
            }
        }

        stage("Sonar Scan") {
            steps {
                script {
                    container('spring-jenkins-slave') {

                        withSonarQubeEnv('production-sonarqube') {
                            sh 'bash gradlew sonar --info'
                        }
                        withSonarQubeEnv('production-sonarqube') {
                            timeout(time: 1, unit: 'HOURS') {
                                // Just in case something goes wrong, pipeline will be killed after a timeout
                                def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
                                if (qg.status != 'OK') {
                                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                                }
                            }
                        }

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
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
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
                String appName = Arrays.asList(REPOSITORY.split("-")).collect({ part -> part.capitalize() }).join(" ")
                String versionString = (version == null)? "null" : version.toVersionCodeString()
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Build for ${appName} on branch ${branchName} building version ${versionString} was ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Spring Build", webhookURL: webhookUrl
                }
            }
        }
    }
}
