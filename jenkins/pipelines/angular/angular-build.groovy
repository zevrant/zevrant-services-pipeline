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
                PGHOST = '10.1.0.18'
            }
            steps {
                script {
                    version = versionService.getVersion(codeUnit.name, true)
                    version = versionService.patchVersionUpdate(codeUnit.name, version, true)
                    currentBuild.displayName = "Building Version ${version.toSemanticVersionString()}" as String
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    gitService.tagVersion(version.toSemanticVersionString(), 'jenkins-git')
                    gradleService.assemble(version)
//                    tar file: "${codeUnit.name}-${version.toSemanticVersionString()}.tar.gz", archive: false, compress: true, dir: "dist/${codeUnit.name}/browser/", glob: "${codeUnit.name}-${version.toSemanticVersionString()}.jar"
                }
            }
        }

        stage('Upload Artifact') {
            environment {
                GH_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    String additionalParams = ''
                    Version previousVersion = versionService.getPreviousVersion(version)
                    if (branchName != 'master') {
                        additionalParams += ' --prerelease'
                    }

                    sh "gh release create --repo ${codeUnit.repo.sshUri} --notes-start-tag ${previousVersion.toSemanticVersionString()} ${version.toSemanticVersionString()} 'build/libs/${codeUnit.name}-${version.toSemanticVersionString()}.jar'"
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                script {
                    writeFile file: 'artifactVersion.txt', text: version.toSemanticVersionString()
                    archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
                }
            }
        }
    }
}