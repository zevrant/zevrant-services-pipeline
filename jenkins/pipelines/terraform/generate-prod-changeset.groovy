package terraform

@Library('CommonUtils')



String versionFileName = 'artifactVersion.txt'
String artifactVersion = ''
pipeline {
    agent {
        kubernetes {
            inheritFrom 'terraform'
        }
    }

    stages {
        stage('Pull Artifact') {
            steps {
                script {
                    container('jnlp') {
                        String[] jobPieces = env.JOB_NAME.split('/')
                        copyArtifacts(filter: versionFileName, projectName: './deploy-to-stage')
                        artifactVersion = readFile(versionFileName)
                        gitTasks.cloneRepository("git@github.zevrant.com:fcs/${jobPieces[jobPieces.length - 2]}.git", artifactVersion)
                        currentBuild.description = "Change set generated for version $artifactVersion."
                    }
                }
            }
        }

        stage("Decrypt TF Vars") {
            steps {
                script {
                    container('gcloud') {
                        gCloud.withContext({
                            terraform.pullAndDecryptTfVars('prod', gcpProject, 'envs/prod',)
                        })
                    }
                }
            }
        }

        stage('Plan Terraform Prod') {
            steps {
                script {
                    container('gcloud') {
                        gCloud.withContext({
                            container('terraform') {
                                withCredentials([string(credentialsId: 'gcp-service-account', variable: 'TF_VAR_service_account_email')]) {
                                    terraform.planTerraform('prod')
                                }
                            }
                        })
                    }
                }
            }
        }

        stage('Generate Change Set Visuals') {
            steps {
                script {
                    container('openjdk11') {
                        sh "./gradlew npmInstall ${gradle.setProxyConfigs().join(' ')}"
                        terraform.generateAndArchiveChangeset('prod')
                    }
                }
            }
        }
    }
}
