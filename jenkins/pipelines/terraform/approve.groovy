package terraform

@Library('CommonUtils') _

import java.lang.reflect.MalformedParametersException

final String versionFileName = 'artifactVersion.txt'
final String inputPassword = params.APPROVAL_PASSWORD
final String overrideVersion = params.OVERRIDE_VERSION
final Boolean confirmOverride = params.CONFIRM_OVERRIDE
final String deployEnvName = env.deployEnvName

BuildManagement buildManagement = TaskLoader.load(binding, BuildManagement.class)
GitTasks gitTasks = TaskLoader.load(binding, GitTasks.class)


pipeline {
    agent {
        label 'jnlp'
    }

    stages {
        stage('Confirm input parameters') {
            steps {
                script {
                    if (confirmOverride) {
                        currentBuild.description = "Approve version ${overrideVersion}"
                        if (overrideVersion.isEmpty()) {
                            throw new MalformedParametersException("Confirm override was selected but no version was supplied")
                        }
                        gitTasks.cloneRepository(fcsApp.repo.getSshUri(), fcsApp.mainBranchName)
                        sshagent(credentials: ['zevrant-github-key']) {
                            sh 'git fetch --tags'
                            String versionOutputFile = 'output.txt'
                            sh "git tag -l ${overrideVersion} | tee ${versionOutputFile}"

                            String output = readFile(file: versionOutputFile)
                            if(output.isBlank()) {
                                throw new RuntimeException("No git tag could be found for version ${overrideVersion}")
                            }
                        }
                    }
                }
            }
        }

        stage('Check approval password') {
            steps {
                script {
                    if (deployEnv.approvalRequiresPassword) {
                        // Fail the build if the password is wrong
                        buildManagement.checkPassword(inputPassword, fcsApp.approvalPasswordCredential)
                    } else {
                        echo "No approval required for this job"
                    }
                }
            }
        }

        stage('Approve the version') {
            steps {
                script {
                    if (confirmOverride) {
                        sh(
                            label: 'Generate override version file',
                            script: "echo ${overrideVersion} > ${versionFileName}" as String
                        )
                        currentBuild.description = "Approve version ${overrideVersion}"
                    } else {
                        copyArtifacts(filter: versionFileName, projectName: './deploy-to-stage')
                        currentBuild.description = "Approve version ${readFile(versionFileName)}"
                    }

                    archiveArtifacts(artifacts: versionFileName, fingerprint: true)
                }
            }
        }
    }
}
