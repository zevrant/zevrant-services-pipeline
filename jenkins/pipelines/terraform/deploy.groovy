package terraform

import com.zevrant.services.pojo.codeunit.TerraformCodeUnit
import com.zevrant.services.pojo.codeunit.TerraformCodeUnitCollection
import com.zevrant.services.services.TerraformService

@Library('CommonUtils')

TerraformService terraformService = new TerraformService(this)
String version = ''
String versionFileName = 'artifactVersion.txt'
TerraformCodeUnit terraformCodeUnit = TerraformCodeUnitCollection.findByRepoName(REPOSITORY)
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Pull Artifact') {
            steps {
                script {
                    copyArtifacts(filter: versionFileName, projectName: "./${terraformCodeUnit.name.split('-').collect({ name -> name.capitalize() }).join(' ')}-multibranch/master")
                    version = readFile(file: versionFileName)
                    sshagent(credentials: [terraformCodeUnit.repo.sshCredentialsId]) {
                        sh "git clone ${terraformCodeUnit.repo.sshUri}"
                        dir(terraformCodeUnit.repo.repoName) {
                            sh 'git fetch --all --tags'
                            sh "git checkout tags/${version}"
                            writeFile(file: 'artifactVersion.txt', text: version)
                            archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                        }
                    }
                    currentBuild.description = "Deploying $version"
                }
            }

        }

        stage("Deploy") {
            environment {
                PGHOST = '10.1.0.18'
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGSSLMODE = 'disable'
                PGDATABASE = 'cicd_tf_backend'
            }
            steps {
                script {
                    dir(terraformCodeUnit.repo.repoName) {
                        terraformService.populateTfEnvVars(terraformCodeUnit, ENVIRONMENT) {
                            terraformService.initTerraform(ENVIRONMENT)
                            terraformService.applyTerraform(ENVIRONMENT)
                        }

                    }
                }
            }
        }

//        stage('Generate Change Set Visuals') {
//            steps {
//                script {
//                    container('openjdk11') {
//                        sh "./gradlew npmInstall ${gradle.setProxyConfigs().join(' ')}"
//                        terraform.generateAndArchiveChangeset(envName)
//                    }
//                }
//            }
//        }
    }
    post {
        success {
            script {
                writeFile(file: versionFileName, text: version)
                archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
            }
        }
    }
}
