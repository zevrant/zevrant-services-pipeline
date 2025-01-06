import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.AngularCodeUnit
import com.zevrant.services.pojo.codeunit.AngularCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.GradleService
import com.zevrant.services.services.VersionService

@Library('CommonUtils')

String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
VersionService versionService = new VersionService(this)
GitService gitService = new GitService(this)
GradleService gradleService = new GradleService(this)

AngularCodeUnit codeUnit = AngularCodeUnitCollection.findCodeUnitByRepositoryName(REPOSITORY)
Version version = null

pipeline {
    agent {
        label 'container-builder'
    }
    stages {
        stage('Test') {
            when { expression { codeUnit.testsEnabled } }
            steps {
                script {
                    gradleService.unitTest()
                }
            }
        }

        stage('Get New Version') {
            environment {
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGHOST = '192.168.0.101'
            }
            steps {
                script {
                    version = versionService.getVersion(codeUnit.name, true)
                    version = versionService.minorVersionUpdate(codeUnit.name, version, true)
                    currentBuild.displayName = "Building Version ${version.toThreeStageVersionString()}" as String
                }
            }
        }

        stage('Build') {
            when { expression { codeUnit.testsEnabled } }
            steps {
                script {
                    gitService.tagVersion(version.toThreeStageVersionString(), 'jenkins-git')
                    gradleService.assemble(version)
                    tar file: "${codeUnit.name}-${version.toThreeStageVersionString()}.tar.gz", archive: false, compress: true, dir: "dist/${codeUnit.name}/browser/"
                }
            }
        }

        stage('Upload Artifact') {
            environment {
                GH_TOKEN = credential('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    String additionalParams = ''
                    String previousVersion = versionService.getPreviousVersion(version)
                    if (branchName != 'master') {
                        additionalParams += ' --prerelease'
                    }

                    sh 'gh auth login --with-token'
                    sh "gh release create --repo ${codeUnit.repo.repoName} --notes-from-tag  --notes-start-tag ${versionService.getPreviousVersion()} -t ${version.toThreeStageVersionString()} ${version.toThreeStageVersionString()}"
                    sh "gh release upload '${codeUnit.name}-${version.toThreeStageVersionString()}.tar.gz'  --repo ${codeUnit.repo.repoName}"
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                script {
                    writeFile file: 'artifactVersion.txt', text: version.toThreeStageVersionString()
                    archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                }
            }
        }
    }
}