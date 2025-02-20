package terraform

import com.zevrant.services.pojo.codeunit.TerraformCodeUnit
import com.zevrant.services.pojo.codeunit.TerraformCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.TerraformService

@Library('CommonUtils')

GitService gitService = new GitService(this)
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
                    container('jnlp') {
                        copyArtifacts(filter: versionFileName, projectName: './Shared-multibranch/master')
                        version = readFile(file: versionFileName)
                        gitService.checkout('git@github.com', 'zevrant', terraformCodeUnit.repo.repoName, version, terraformCodeUnit.getRepo().getSshCredentialsId())
                        currentBuild.description = "Deploying $artifactVersion"
                    }
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
                    terraformCodeUnit.envs.each { env ->
                        terraformService.populateTfEnvVars(terraformCodeUnit, env) {
                            terraformService.initTerraform(env)
                            terraformService.applyTerraform(env)
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
