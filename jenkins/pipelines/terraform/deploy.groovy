package terraform

@Library('CommonUtils')

String repository = JOB_NAME.split('/')[JOB_NAME.split('/').length - 2]
final String envName = deployEnvName.replace('-internal', '')
final String bucketName = 'bkt-tfstate-ft'

//Determine source job to copy version from
if (envName != 'stage' && envName != 'prod') {
    if (versionSourceJob == null || versionSourceJob == '') {
        throw new RuntimeException('No Source Job Provided')
    }
    println "Is PR? ${((String) versionSourceJob).matches('PR-\\d+')}"
    println "Is master? ${versionSourceJob == 'multibranch/master'}"
    println "Is main? ${versionSourceJob == 'multibranch/main'}"
    if (!((String) versionSourceJob).matches('PR-\\d+')
        && !(versionSourceJob == 'multibranch/master' || versionSourceJob == 'multibranch/main')) {
        throw new RuntimeException("Invalid Source Job Provided ${versionSourceJob}")
    }
}

String versionFileName = 'artifactVersion.txt'
String artifactVersion = (params.ARTIFACT_VERSION != null && params.ARTIFACT_VERSION != '')
    ? params.ARTIFACT_VERSION
    : ''
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

                        if (artifactVersion == '') {
                            String sourceJob = "./${versionSourceJob}"
                            if (envName != 'sandbox' && envName != 'dev' && envName != 'qa') {
                                sourceJob = versionSourceJob
                            }
                            if (sourceJob == 'multibranch/release') {
                                sourceJob = 'deploy-to-dev'
                            }
                            sourceJob = sourceJob.replace('-internal', '')
                            copyArtifacts(filter: versionFileName, projectName: sourceJob)
                            artifactVersion = readFile(versionFileName)
                        } else {
                            println("Not copying artifact from job, direct version was supplied artifact version :${artifactVersion}.")
                        }
                        gitTasks.cloneRepository("git@github.zevrant.com:fcs/${repository}.git", artifactVersion, true)
                        currentBuild.description = "Deploying $artifactVersion"
                    }
                }
            }

        }

        stage("Decrypt TF Vars") {
            steps {
                script {
                    container('gcloud') {
                        gcloud.withContext(gcpProject, {
                            terraform.pullAndDecryptTfVars(envName, gcpProject, "envs/${envName}")
                        })
                    }
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    container('gcloud') {
                        gcloud.withContext(gcpProject, {
                            container('terraform') {
                                terraform.applyTerraform(envName)
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
                        terraform.generateAndArchiveChangeset(envName)
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                writeFile(file: versionFileName, text: artifactVersion)
                archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
            }
        }
    }
}
