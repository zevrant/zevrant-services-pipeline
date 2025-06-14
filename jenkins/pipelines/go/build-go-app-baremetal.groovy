package go

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
isPullRequest = BRANCH_NAME.startsWith('PR-')
String branchName = (isPullRequest) ? CHANGE_BRANCH : BRANCH_NAME
Version version = null
GoCodeUnit codeUnit = new GoCodeUnitCollection().findCodeUnitByRepositoryName(REPOSITORY)
String artifactVersion = ''
isCurrentCommitAlreadyTagged = false
if (JOB_URL.contains('/Sandboxes/')) {
    env.sandboxMode = 'SANDBOX-MODE'
}
pipeline {
    agent {
        label 'container-builder'
    }

    stages {

        stage('Get New Version') {
            environment {
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGHOST = '10.1.0.18'
            }
            steps {
                script {
                    version = versionService.getVersion(codeUnit.name, true)
                    if (isPullRequest) {
                        version = versionService.getBuildVersion(codeUnit.name, version, true)
                    } else {
                        version = versionService.patchVersionUpdate(codeUnit.name, version, true)
                    }
                    currentBuild.displayName = "Building Version ${version.toSemanticVersionString()}" as String
                    artifactVersion = version.toSemanticVersionString()
                    writeFile(file: 'artifactVersion.txt', text: "v${artifactVersion}" as String)
                    archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                }
            }
        }

        stage('SCM Checkout') {
            steps {
                script {
                    sh 'git config --global --add safe.directory "$(pwd)"'
                    String cloneUrl = codeUnit.repo.sshUri
                    println("Clone Url: ${cloneUrl}")
                    gitService.checkout("git@github.com", codeUnit.getRepo().getOrg(), codeUnit.repo.repoName, branchName, codeUnit.getRepo().getSshCredentialsId(), false, false)
                    sh ''' echo '[url "ssh://git@github.com/"]
        insteadOf = https://github.com/
        [url "ssh://git@git.zevrant-services.com:30121/"]
            insteadOf = https://git.zevrant-services.com/' >> ~/.gitconfig
'''

                }
            }
        }
        stage('Unit Test') {
            when { expression { codeUnit.unitTestsEnabled } }
            steps {
                script {
                    sshagent([codeUnit.getRepo().getSshCredentialsId()]) {
//                        if (codeUnit.applicationType == ApplicationType.GO_HELM && codeUnit.swaggerEnabled) {
//                            sh 'swag init'
//                        }
                        //runs go tests for all packages and generates report in junit xml format that jenkins can read
                        try {
//                            writeFile(file: 'known_hosts', text: gitService.getApprovedKnownHosts())
//                            sh 'mkdir -p ~/.ssh/'
//                            sh 'mv known_hosts ~/.ssh/known_hosts'
//                            sh(
//                                    label: 'Set Jenkins user name / email',
//                                    script: '''#!/bin/bash -xe
//                                        git config --global user.email "jenkins@zevrant-services.com"
//                                        git config --global user.name "Zevrant Services Jenkins"
//                                    '''.stripIndent()
//                            )
//
//                            sh 'git config --global --add safe.directory "$(pwd)"'
                            if (fileExists(file: "./generateMocks.sh")) {
                                sh './generateMocks.sh'
                            }
                            sh 'gotestsum --format pkgname --junitfile report.xml -- -failfast -race -coverprofile=coverage.out ./...'
                        } finally {
                            sh 'ls -l'
                            junit allowEmptyResults: true, keepLongStdio: true, skipPublishingChecks: true, testResults: 'report.xml'
                        }
                        sh 'rm -f coverage.out report.xml *_mock.go *_mock_test.go'
                    }
                }
            }
        }


        stage("Tag Release") {
            steps {
                script {
                    sshagent(credentials: [codeUnit.repo.sshCredentialsId]) {
                        writeFile(file: 'known_hosts', text: """# github.com:22 SSH-2.0-c541a10de
github.com ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCj7ndNxQowgcQnjshcLrqPEiiphnt+VTTvDP6mHBL9j1aNUkY4Ue1gvwnGLVlOhGeYrnZaMgRK6+PKCUXaDbC7qtbW8gIkhL7aGCsOr/C56SJMy/BCZfxd1nWzAOxSDPgVsmerOBYfNqltV9/hWCqBywINIR+5dIg6JTJ72pcEpEjcYgXkE2YEFXV1JHnsKgbLWNlhScqb2UmyRkQyytRLtL+38TGxkxCflmO+5Z8CSSNY7GidjMIZ7Q4zMjA2n1nGrlTDkzwDCsw+wqFPGQA179cnfGWOWRVruj16z6XyvxvjJwbz0wQZ75XK5tKSb7FNyeIEs4TT4jk+S4dhPeAUC5y+bDYirYgM4GC7uEnztnZyaVWQ7B381AK4Qdrwt51ZqExKbQpTUNn+EjqoTwvqNj4kqx5QUCI0ThS/YkOxJCXmPUWZbhjpCg56i+2aB6CmK2JGhn57K5mj0MNdBXA4/WnwH6XoPWJzK5Nyu2zB3nAZp+S5hpQs+p1vN1/wsjk=
# github.com:22 SSH-2.0-c541a10de
github.com ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEmKSENjQEezOmxkZMy7opKgwFB9nkt5YRrYMjNuG5N87uRgg6CLrbo5wAdT/y6v0mKV0U2w0WZ2YB/++Tpockg=
# github.com:22 SSH-2.0-c541a10de
github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
# github.com:22 SSH-2.0-c541a10de
# github.com:22 SSH-2.0-c541a10de""")
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
                        sh "git tag -f v${artifactVersion} && git push -f origin v${artifactVersion}"
                        sh 'git status'
                    }
                }
            }
        }

        stage('Release Version') {
            environment {
                GPG_FINGERPRINT = "65B4607F0CB9810D48F2012B0CFB08076C3770BF"
                GPG_PASSWORD = credentials('terraform-gpg-password')
                GPG_KEY = credentials('gpg-key')
                GORELEASER_CURRENT_TAG = "v$artifactVersion"
//                GORELEASER_PREVIOUS_TAG = "v${artifactVersion.split("\\+")[0]}"
            }
            steps {
                script {
                    sh 'gpg -v --batch --import $GPG_KEY'
                    sh 'echo "$GPG_PASSWORD" | gpg -v --batch --yes --passphrase-fd 0 --pinentry-mode loopback --clearsign main.go'

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
                            withCredentials([string(credentialsId: 'jenkins-git-access-token-as-text', variable: 'password')]) {
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
    post {
        always {
            script {
                try {
//                    if (branchName != 'main') {
//                        pullRequest.comment("Failure Logs Can be Found [Here](${env.BUILD_URL}/console)")
//                    }

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