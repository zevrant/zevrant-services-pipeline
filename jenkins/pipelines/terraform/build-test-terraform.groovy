package terraform

@Library("CommonUtils") _

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME

if((branchName == 'master' || branchName == 'main') && (REPOSITORY.toLowerCase() == 'gcp-infrastructure-as-code' || REPOSITORY.toLowerCase() == 'kafka-terraform')) {
    environments = ['dev': commonGcpProject, 'qa': commonGcpProject, 'stage': commonGcpProject, 'prod': commonGcpProject]
} else if(REPOSITORY.toLowerCase() == 'gcp-infrastructure-as-code') {
    environments = ['sandbox': commonGcpProject]
} else if ((branchName == 'master' || branchName == 'main')) {
    environments =  [
        'dev'  : GcpProjectCollection.findByDisplayName('dev', true),
        'qa'   : GcpProjectCollection.findByDisplayName('qa', true),
        'stage': GcpProjectCollection.findByDisplayName('stage', true),
        'prod' : GcpProjectCollection.findByDisplayName('prod', true)
    ]
} else {
    environments = ['sandbox': GcpProjectCollection.findByDisplayName('sandbox', true)]
}


String version = ""
pipeline {
    agent {
        kubernetes {
            inheritFrom 'terraform'
        }
    }
    stages {
        stage('SCM Checkout') {
            steps {
                script {
                    container('jnlp') {

                        //PRs should first be deployed to sandbox, as some error can only be caught on deploy
                        //This is explicitly pulled because the clone by the multibranch doesn't make the
                        //ability to push changes available, I think it hides or removes the .git folder
                        if (branchName != 'master' && branchName != 'main') {
                            git url: "git@github.zevrant.com:fcs/${REPOSITORY}.git" as String, credentialsId: 'zevrant-github-key', branch: 'sandbox'
                            sshagent(credentials: ['zevrant-github-key']) {
                                sh(
                                    label: 'Set Jenkins user name / email',
                                    script: '''#!/bin/bash -xe
                                        git config --global user.email "zevrant@zevrant.com"
                                        git config --global user.name "FCS Jenkins"
                                    '''.stripIndent()
                                )
                                sh """
                                        git merge -X theirs origin/${fcsApp.mainBranchName}
                                        git merge origin/${branchName}
                                        git remote remove origin
                                        git remote add origin git@github.zevrant.com:fcs/${REPOSITORY}.git
                                        git push origin HEAD
                                    """
                            }
                        } else {
                            try {
                                gitTasks.cloneRepository("git@github.zevrant.com:fcs/${REPOSITORY}.git", 'master')
                            } catch (Exception ex) {
                                println "ERROR: ${ex.getMessage()}"
                                ex.printStackTrace()
                                try {
                                    gitTasks.cloneRepository("git@github.zevrant.com:fcs/${REPOSITORY}.git", 'main')
                                } catch (Exception ex2) {
                                    throw ex2
                                }
                            }
                        }
                    }
                }
            }
        }

        stage("Decrypt TF Vars") {
            steps {
                script {
                    container('gcloud') {
                        Map<String, Closure> steps = new HashMap<>()
                        environments.keySet().each({ environment ->
                            steps.put(environment, {
                                gcloud.withContext(gcpProject, {
                                    terraform.pullAndDecryptTfVars(environment, gcpProject,"envs/${environment}", )
                                })
                            })
                        })
                        parallel steps
                    }
                }
            }
        }

        stage('Build Terraform') {
            steps {
                script {
                    container('gcloud') {
                        Map<String, Closure> steps = new HashMap<>()
                        environments.keySet().each { environment ->
                            steps.put(environment, {
                                println "Operating on the ${environment} environment"
                                println "Obtained Gcp Project with id ${gcpProject.id}"
                                gcloud.withContext(gcpProject, {
                                    container('terraform') {
                                        withEnv(['TF_VAR_service_account_email=' + gcpProject.serviceAccount]) {
                                            terraform.planTerraform(environment)
                                        }
                                    }
                                })
                            })
                        }
                        parallel steps
                    }
                }
            }
        }
        stage('Unit Test') { //TODO switch to terraform validator
            when { expression {  false } }
            //some serious optimization needs to be done here, not that many tests and they take minutes to run
            steps {
                script {
                    container('openjdk11') {
                        sh "./gradlew clean test -Penvironment=dev --no-watch-fs --info ${gradle.setProxyConfigs().join(' ')}"
                    }
                }
            }
        }

        stage('Generate Change Set Visuals') {
            steps {
                script {
                    container('openjdk11') {
                        sh "./gradlew npmInstall ${gradle.setProxyConfigs().join(' ')}"
                        Map<String, Closure> steps = new HashMap<>()
                        environments.keySet().each { environment ->
                            steps.put("Visualize ${environment.capitalize()}" as String, {
                                terraform.generateAndArchiveChangeset(environment)
                            })
                        }
                        parallel steps
                    }
                }
            }
        }

        stage('Create Deployment Artifact') {
            steps {
                script {
                    container('jnlp') {
                        sshagent(credentials: ['zevrant-github-key']) {
                            version = VersionGenerator.generate(branchName);
                            sh "git tag ${version}"
                            sh 'git push origin --tags'
                            writeFile(file: 'artifactVersion.txt', text: version)
                            archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                        }
                    }
                }
            }
        }

        stage('Trigger Sandbox Deploy') {
            when { expression { branchName != 'master' } }
            steps {
                script {
                    String[] jobPieces = JOB_NAME.split('/')
                    build(
                        job: '../deploy-to-sandbox',
                        propagate: true,
                        wait: true,  //many errors can be deploy time only
                        parameters: [
                            [$class: 'StringParameterValue', name: 'SOURCE_JOB', value: jobPieces[jobPieces.length - 1]],
                            [$class: 'StringParameterValue', name: 'ARTIFACT_VERSION', value: version],
                        ]
                    )
                }
            }
        }

        stage('Trigger Preprod Deploy') {
            when { expression { branchName == 'master' || branchName == 'main' } }
            steps {
                script {
                    build(
                        job: '../deploy-to-dev',
                        propagate: true,
                        wait: true,
                        parameters: [
                            [$class: 'StringParameterValue', name: 'ARTIFACT_VERSION', value: version],
                        ]
                    )
                    build(
                        job: '../deploy-to-qa',
                        propagate: true,
                        wait: true,
                        parameters: [
                            [$class: 'StringParameterValue', name: 'ARTIFACT_VERSION', value: version]
                        ]
                    )
                }
            }

        }
    }
    post {
        always {
            script {
                container('jnlp') {
                    sh 'set +e rm -rf *'
                    gitHub.postBuildPrHook("git@github.zevrant.com:fcs/${REPOSITORY}.git")
                }
            }
        }
    }
}


