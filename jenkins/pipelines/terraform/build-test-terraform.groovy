package terraform


import com.zevrant.services.pojo.codeunit.TerraformCodeUnit
import com.zevrant.services.pojo.codeunit.TerraformCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.TerraformService
import com.zevrant.services.services.VersionService

@Library("CommonUtils") _

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
TerraformCodeUnit terraformCodeUnit = TerraformCodeUnitCollection.findByRepoName(REPOSITORY)
TerraformService terraformService = new TerraformService(this)
VersionService versionService = new VersionService(this)
GitService gitService = new GitService(this)
boolean bareMetal = terraformCodeUnit.bareMetal

String version = ""
pipeline {
    agent {
        label 'container-builder'
    }
    stages {
        stage('SCM Checkout') {
            steps {
                script {
                    gitService.checkout('git@github.com', 'zevrant', terraformCodeUnit.repo.repoName, branchName, terraformCodeUnit.getRepo().getSshCredentialsId())

                }
            }
        }


        stage('Build Terraform') {
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
                            terraformService.planTerraform(env)
                        }

                    }
                }
            }
        }

        stage('Get New Version') {
            environment {
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGHOST = '10.1.0.18'
            }
            steps {
                script {
                    version = versionService.getVersion(terraformCodeUnit.name, true)
                    version = versionService.minorVersionUpdate(terraformCodeUnit.name, version, true)
                    currentBuild.displayName = "Building Version ${version.toThreeStageVersionString()}" as String
                }
            }
        }


        stage('Unit Test') { //TODO switch to terraform validator
            when { expression { false } }
            //some serious optimization needs to be done here, not that many tests and they take minutes to run
            steps {
                script {
                    container('openjdk11') {
                        sh "./gradlew clean test -Penvironment=dev --no-watch-fs --info ${gradle.setProxyConfigs().join(' ')}"
                    }
                }
            }
        }

//        stage('Generate Change Set Visuals') {
//            steps {
//                script {
//                    container('openjdk11') {
//                        sh "./gradlew npmInstall ${gradle.setProxyConfigs().join(' ')}"
//                        Map<String, Closure> steps = new HashMap<>()
//                        environments.keySet().each { environment ->
//                            steps.put("Visualize ${environment.capitalize()}" as String, {
//                                terraform.generateAndArchiveChangeset(environment)
//                            })
//                        }
//                        parallel steps
//                    }
//                }
//            }
//        }

        stage('Create Deployment Artifact') {
            steps {
                script {
                    container('jnlp') {
                        sshagent(credentials: [terraformCodeUnit.repo.sshCredentialsId]) {
                            sh "git tag ${version}"
                            sh 'git push origin --tags'
                            writeFile(file: 'artifactVersion.txt', text: version)
                            archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                        }
                    }
                }
            }
        }
    }
}


