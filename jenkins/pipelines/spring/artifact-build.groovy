@Library("CommonUtils") _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.ServiceLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.services.GradleService
import com.zevrant.services.services.KubernetesService
import com.zevrant.services.services.VersionService

List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
VersionService versionService = new VersionService(this)
KubernetesService kubernetesService = new KubernetesService(this)
GradleService gradleService = new GradleService(this)
Version version = null
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
SpringCodeUnit codeUnit = SpringCodeUnitCollection.findByRepoName(REPOSITORY)
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage("Build Microservice") {
            environment {
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
//                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
//                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
//                AWS_DEFAULT_REGION = "us-east-1"
//                DOCKER_TOKEN = credentials('jenkins-harbor')
//            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        sh "CI=ci bash gradlew assemble -x test -x integrationTest --no-watch-fs"
                    }
                }
            }
        }

        stage("Test") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    String username = ""
                    String password = ""
                    container('kubectl') {
                        username = kubernetesService.getSecretValue("${REPOSITORY}-vault-credentials", 'username', 'develop')
                        password = kubernetesService.getSecretValue("${REPOSITORY}-vault-credentials", 'password', 'develop')
                    }
                    container('spring-jenkins-slave') {
                        writeFile(file: '/var/zevrant-services/vault/username', text: username)
                        writeFile(file: '/var/zevrant-services/vault/password', text: password)
                        sh 'openssl rand 256 | base64 -w 0 > /var/zevrant-services/keystore/password'
                        sh 'openssl ecparam -genkey -name prime256v1 -genkey -noout -out private.pem'
                        sh 'openssl req -new -x509 -key private.pem -out certificate.pem -days 900000 -subj "/C=PL/ST=Silesia/L=Katowice/O=MyOrganization/CN=CommonName"'
                        sh 'openssl pkcs12 -export -inkey private.pem -in certificate.pem -passout "file:/var/zevrant-services/keystore/password" -out /opt/acme/certs/zevrant-services.p12'
                        sh "SPRING_PROFILES_ACTIVE='develop,test' bash gradlew test --no-watch-fs --info"
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }

        }

        stage("Integration Test") {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
                GITEA_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    container('spring-jenkins-slave') {
                        sh './gradlew integrationTest -Pcicd=true --info'
                        junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'build/test-results/integrationTest/*.xml'
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
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
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

//        stage("Build Artifact") {
//            environment {
//                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
//                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
//                AWS_DEFAULT_REGION = "us-east-1"
//                DOCKER_TOKEN = credentials('jenkins-harbor')
//            }
//            steps {
//                script {
//                    timeout(time: 20, unit: 'MINUTES') {
//                        container('buildah') {
//                            String versionString = (branchName == "main")
//                                    ? version.toVersionCodeString()
//                                    : "${version.toVersionCodeString()}-${branchName}" as String
//                            def appYaml = readYaml(file: 'src/main/resources/application.yml')
//                            String containerPort = appYaml.server.port
//                            sh 'echo $DOCKER_TOKEN | buildah login -u \'robot$jenkins\' --password-stdin docker.io'
//                            sh "buildah bud --build-arg serviceName=$REPOSITORY --build-arg containerPort=$containerPort -t harbor.zevrant-services.internal/zevrant-services/$REPOSITORY:${versionString} ."
//                            sh "buildah push harbor.zevrant-services.internal/zevrant-services/$REPOSITORY:${versionString}"
//                        }
//                    }
//                }
//            }
//        }

    }
    post {
        success {
            script {
                tar(file: 'helm-chart.tgz', archive: true, compress: true, glob: 'helm/**/*')
                writeFile(file: 'artifactVersion.txt', text: version.toVersionCodeString())
                archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
            }
        }
        always {
            script {
                String appName = "${REPOSITORY.split('-').collect {part -> part.capitalize()}.join(' ')}"
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Build for ${appName} on branch ${branchName} ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Spring Build", webhookURL: webhookUrl
                }
                junit allowEmptyResults: true, checksName: 'JUnit Tests', testResults: 'build/test-results/*/*.xml'
            }
        }
    }
}
