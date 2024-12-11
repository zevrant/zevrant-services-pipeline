import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.GoCodeUnit
import com.zevrant.services.pojo.codeunit.GoCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.VersionService

@Library('CommonUtils') _


GitService gitService = new GitService(this)
VersionService versionService = new VersionService(this)
//NotificationsService notificationsService = new NotificationsService(this)

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME

GoCodeUnit codeUnit = new GoCodeUnitCollection().findCodeUnitByRepositoryName(REPOSITORY)
String artifactVersion = ''

if (JOB_URL.contains('/Sandboxes/')) {
    env.sandboxMode = 'SANDBOX-MODE'
}
pipeline {
    agent {
        kubernetes {
            inheritFrom 'golang'
        }
    }

    stages {

        stage('SCM Checkout') {
            steps {
                script {
                    container('golang') {
                        sh 'git config --global --add safe.directory "$(pwd)"'
                        String cloneUrl = codeUnit.repo.sshUri
                        if (codeUnit.repo.httpsUri.contains("git.cgdevops")) {
                            cloneUrl = codeUnit.repo.sshUrl
                        }
                        println("Clone Url: ${cloneUrl}")
                        gitService.checkout(cloneUrl, branchName, true, codeUnit.getRepo().getSshCredentialsId())
                        sh ''' echo '[url "ssh://git@github.com/"]
        insteadOf = https://github.com/
        [url "ssh://git@git.zevrant-services.com:30121/"]
            insteadOf = https://git.zevrant-services.com/' >> ~/.gitconfig
'''

                    }
                }
            }
        }
        stage('Unit Test') {
            steps {
                script{
                    container('golang') {
                        sshagent([codeUnit.getRepo().getSshCredentialsId()]) {
                            if (codeUnit.applicationType == ApplicationType.GO_HELM && codeUnit.swaggerEnabled) {
                                sh 'swag init'
                            }
                            //runs go tests for all packages and generates report in junit xml format that jenkins can read
                            try {
                                writeFile(file: 'known_hosts', text: gitService.getApprovedKnownHosts())
                                sh 'mkdir -p ~/.ssh/'
                                sh 'mv known_hosts ~/.ssh/known_hosts'
                                sh(
                                        label: 'Set Jenkins user name / email',
                                        script: '''#!/bin/bash -xe
                                        git config --global user.email "jenkins@zevrant-services.com"
                                        git config --global user.name "Zevrant Services Jenkins"
                                    '''.stripIndent()
                                )

                                sh 'git config --global --add safe.directory "$(pwd)"'
                                sh 'gotestsum --format pkgname --junitfile report.xml -- -failfast -race -coverprofile=coverage.out ./...'
                            } finally {
                                sh 'ls -l'
                                junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'report.xml'
                            }
                            sh 'rm -f coverage.out report.xml'
                        }
                    }
                }
            }
        }


        stage("Increment Minor Version") {
            steps {
                script {
                    container('mongosh') {
                        Version currentVersion = versionService.getVersion(codeUnit.name)
                        if (branchName == codeUnit.getDefaultBranch()) {
                            currentVersion = versionService.minorVersionUpdate(codeUnit.name, currentVersion)
                        }
                        artifactVersion = currentVersion.toThreeStageVersionString()
                    }
                }
            }
        }

        stage("Tag Release") {
            steps {
                script {
                    container('golang') { //this container needs git to be able to read commit history and tags
                        sshagent(credentials: [codeUnit.repo.sshCredentialsId]) {
                            writeFile(file: 'known_hosts', text: gitService.getApprovedKnownHosts())
                            sh 'mkdir -p ~/.ssh/'
                            sh 'mv known_hosts ~/.ssh/known_hosts'
                            sh(
                                    label: 'Set Jenkins user name / email',
                                    script: '''#!/bin/bash -xe
                                        git config --global user.email "jenkins@zevrant-services.com"
                                        git config --global user.name "Zevrant Services Jenkins"
                                    '''.stripIndent()
                            )
                            if (branchName != codeUnit.getDefaultBranch()) {
                                artifactVersion = gitService.getNextBetaTagForVersion(artifactVersion)
                            }
                            sh 'git config --global --add safe.directory "$(pwd)"'
                            sh "git tag -f v${artifactVersion} && git push -f origin v${artifactVersion}"
                            sh 'git status'
                        }
                    }
                }
            }
        }

        stage('Release Version') {
            steps {
                script {
                    container('golang') {
                        //no spillage here as the key var is just the path to the key
                        println 'cleaning up untracked files'

                        gitService.cleanUntrackedFiles();
                        sshagent([codeUnit.getRepo().getSshCredentialsId()]) {
                            if (codeUnit.repo.sshCredentialsId.contains('gitea')) {
                                withCredentials([usernamePassword(credentialsId: codeUnit.getRepo().sshCredentialsId, passwordVariable: 'password', usernameVariable: 'username')]) {
                                    withEnv(['GITEA_TOKEN=' + password]) {
                                        sh "goreleaser release --clean"
                                    }
                                }
                            } else {
                                withCredentials([usernamePassword(credentialsId: codeUnit.getRepo().sshCredentialsId, passwordVariable: 'password', usernameVariable: 'username')]) {
                                    withEnv(['GITHUB_TOKEN=' + password]) {
                                        sh "goreleaser release --clean"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
    post {
        always {
            script {
                try {
//                    if (branchName != 'main') {
//                        pullRequest.comment("Failure Logs Can be Found [Here](${env.BUILD_URL}/console)")
//                    }
                    writeFile(file: 'artifactVersion.txt', text: "v${artifactVersion}" as String)
                    archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
//                    if (codeUnit.staticResourceSource != 'garbage') {
//                        sh 'ls -l'
//                        archiveArtifacts(artifacts: "${codeUnit.staticResourceSource}.zip", allowEmptyArchive: false)
//                    }
                } finally {
//                    notificationsService.sendTeamsBuildNotification(artifactVersion, 'Multibranch App Build', codeUnit, TeamsChannel.ARTIFACT_BUILD_STATUS)

                }
            }
        }
    }
}