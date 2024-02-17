import com.zevrant.services.pojo.Version
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.VersionService

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME

Version version;
VersionService versionTasks = new VersionService(this)
KubernetesService kubernetesService = new KubernetesService(this)
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }
    stages {

        stage("Assemble") {
                    imageBuildService.registryLogin(DOCKER_CREDENTIALS_USR, DOCKER_CREDENTIALS_PSW, 'harbor.zevrant-services.internal')
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
                GRADLE_CACHE_CREDENTIALS = credentials('gradle-build-cache')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        sh 'buildCache { >> gradle.properties'
                        sh 'remote(HttpBuildCache) { >> gradle.properties'
                        sh 'url = \'https://example.com:8123/cache/\' >> gradle.properties'
                        sh 'credentials { >> gradle.properties'
                        sh 'username = "$GRADLE_CACHE_CREDENTIALS_USR" >> gradle.properties'
                        sh 'password = "$GRADLE_CACHE_CREDENTIALS_PSW" >> gradle.properties'
                        sh '}}} >> gradle.properties'
                        sh "bash gradlew clean assemble --build-cache --info"
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
                        sh "bash gradlew test --build-cache --info"
                    }
                }
            }
        }

        stage("Integration Test") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    String username = ""
                    String password = ""
                    String keycloakPassword = ""

                    container('kubectl') {
                        retry(3, {
                            username = kubernetesService.getSecretValue("jenkins-vault-credentials", 'username', 'jenkins')
                        })
                        retry(3, {
                            password = kubernetesService.getSecretValue("jenkins-vault-credentials", 'password', 'jenkins')
                        })
                        keycloakPassword = kubernetesService.getSecretValue("test-admin-keycloak-credentials", 'password', 'jenkins')
                    }
                    container('spring-jenkins-slave') {
                        writeFile(file: '/var/zevrant-services/vault/username', text: username)
                        writeFile(file: '/var/zevrant-services/vault/password', text: password)
                        sh 'openssl rand 256 | base64 -w 0 > /var/zevrant-services/keystore/password'
                        sh 'openssl ecparam -genkey -name prime256v1 -genkey -noout -out private.pem'
                        sh 'openssl req -new -x509 -key private.pem -out certificate.pem -days 900000 -subj "/C=PL/ST=Silesia/L=Katowice/O=MyOrganization/CN=CommonName"'
                        sh 'openssl pkcs12 -export -inkey private.pem -in certificate.pem -passout "file:/var/zevrant-services/keystore/password" -out /opt/acme/certs/zevrant-services.p12'
                        sh "bash gradlew integrationTest --build-cache --info"
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

        stage("Sonar Scan") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {

                        withSonarQubeEnv('production-sonarqube') {
                            sh "bash gradlew sonar -PprojVersion=${version.toVersionCodeString()} --info"
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
                        sh "bash gradlew clean assemble publish -PprojVersion=${version.toVersionCodeString()} --no-daemon --build-cache"
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                String appName = Arrays.asList(REPOSITORY.split("-")).collect({ part -> part.capitalize() }).join(" ")
                String versionString = (version == null) ? "null" : version.toVersionCodeString()
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Build for ${appName} on branch ${branchName} building version ${versionString} was ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Spring Build", webhookURL: webhookUrl
                }
            }
        }
    }
}
