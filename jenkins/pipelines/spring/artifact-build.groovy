@Library("CommonUtils") _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.GradleService
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.NotificationService
import com.zevrant.services.services.VersionService

List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
VersionService versionService = new VersionService(this)
KubernetesService kubernetesService = new KubernetesService(this)
GradleService gradleService = new GradleService(this)
GitService gitService = new GitService(this)

Version version = null
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
SpringCodeUnit codeUnit = SpringCodeUnitCollection.findByRepoName(REPOSITORY)
boolean containsCodeChanges = true
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage ('Determine Code Changes') {
            steps{
                script {
                    container ('jnlp') {
                        //return true if there are no files changes within src/main/java
                        List<String> filesChanged = gitService.getFilesChanged()
                        println filesChanged.join(' ')
                        filesChanged = filesChanged.findAll({file -> file.contains('src/main/java')})
                        println(filesChanged.join(' '))
                        containsCodeChanges = !filesChanged.isEmpty()
                    }
                }
            }
        }

        stage("Build Microservice") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
//                GRADLE_CACHE_CREDENTIALS = credentials('gradle-cache-credentials')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
//                        sh 'echo "buildCache {" >> settings.gradle'
//                        sh 'echo "remote(HttpBuildCache) {" >> settings.gradle'
//                        sh 'echo "url = \'https://build-cache-node:5071/cache/\'" >> settings.gradle'
//                        sh 'echo "allowUntrustedServer = true" >> settings.gradle'
//                        sh 'echo "credentials {" >> settings.gradle'
//                        sh 'echo "username = \'$GRADLE_CACHE_CREDENTIALS_USR\'" >> settings.gradle'
//                        sh 'echo "password = \'$GRADLE_CACHE_CREDENTIALS_PSW\'" >> settings.gradle'
//                        sh 'echo "}}}" >> settings.gradle'
                        sh "CI=ci bash gradlew assemble -x test -x integrationTest --no-watch-fs"
                    }
                }
            }
        }

        stage("Test") {
            when { expression { containsCodeChanges } }
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
                            username = kubernetesService.getSecretValue("${REPOSITORY}-vault-credentials", 'username', 'develop')
                        })
                        retry(3, {
                            password = kubernetesService.getSecretValue("${REPOSITORY}-vault-credentials", 'password', 'develop')
                        })
                        retry(3, {
                            keycloakPassword = kubernetesService.getSecretValue("test-admin-keycloak-credentials", 'password', 'jenkins')
                        })

                    }
                    container('spring-jenkins-slave') {
                        def stages = [
                                "Unit Test": {
                                    stage("Unit Test") {
                                        writeFile(file: '/var/zevrant-services/vault/username', text: username)
                                        writeFile(file: '/var/zevrant-services/vault/password', text: password)
                                        sh 'openssl rand 256 | base64 -w 0 > /var/zevrant-services/keystore/password'
                                        sh 'openssl ecparam -genkey -name prime256v1 -genkey -noout -out private.pem'
                                        sh 'openssl req -new -x509 -key private.pem -out certificate.pem -days 900000 -subj "/C=PL/ST=Silesia/L=Katowice/O=MyOrganization/CN=CommonName"'
                                        sh 'openssl pkcs12 -export -inkey private.pem -in certificate.pem -passout "file:/var/zevrant-services/keystore/password" -out /opt/acme/certs/zevrant-services.p12'
                                        sh "SPRING_PROFILES_ACTIVE='develop,test' bash gradlew test -x integrationTest -x jacocoTestReport --no-watch-fs --info --build-cache"
                                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'build/test-results/test/*.xml'
                                    }
                                },
                                "IntegrationTest": {
                                    stage("Integration Test") {
                                        sh './gradlew integrationTest -Pcicd=true --build-cache --info'
                                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'build/test-results/integrationTest/*.xml'
                                    }
                                }
                        ]
                        parallel(stages)
                    }
                }
            }

        }


        stage("Sonar Scan") {
            when { expression { containsCodeChanges } }
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        withSonarQubeEnv('production-sonarqube') {
                            sh 'bash gradlew jacocoTestReport sonar'
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
                        version = versionService.getVersion(REPOSITORY as String) as Version
                        versionCode = versionService.getVersion("${REPOSITORY.toLowerCase()}-code")
                        currentBuild.displayName = "Building version ${version.toVersionCodeString()}"
                    }
                }
            }
        }

        stage("Version Update") {
            when { expression { branchName == "main" } }
            environment {
                REDISCLI_AUTH = credentials('jenkins-keydb-password')
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        version = versionService.incrementVersion(REPOSITORY, version);
                        currentBuild.displayName = "Building version ${version.toVersionCodeString()}"
                    }
                }
            }
        }

        stage('Publish Jar') {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
//                        lock(resource: "${codeUnit.name}-version" as String, quantity: 1) {
//                        }
                        gradleService.publish(version, codeUnit)
                    }
                }
            }
        }

    }
    post {
        success {
            script {
                tar(file: 'helm-chart.tgz', archive: true, compress: true, glob: 'helm/**/*')
                writeFile(file: 'artifactVersion.txt', text: version.toVersionCodeString())
                archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)

                build job: "../container-build", wait: false
            }
        }
        always {
            script {
                String appName = "${REPOSITORY.split('-').collect { part -> part.capitalize() }.join(' ')}"
                new NotificationService(this).sendDiscordNotification("Jenkins Build for ${appName} on branch ${branchName} ${currentBuild.currentResult}",
                        env.BUILD_URL,
                        currentBuild.currentResult,
                        "Spring Build",
                        NotificationChannel.DISCORD_CICD
                )
                junit allowEmptyResults: true, checksName: 'JUnit Tests', testResults: 'build/test-results/*/*.xml'
            }
        }
    }
}
