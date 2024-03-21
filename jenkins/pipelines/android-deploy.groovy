import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.services.NotificationService

@Library("CommonUtils") _

pipeline {
    agent {
        kubernetes {
            inheritFrom "jnlp"
        }
    }

    stages {

        stage("Get Artifact") {
            steps {
                script {
                    String artifactJob = (ENVIRONMENT == 'prod')
                            ? 'Android/zimage/ZImage-Release-To-Internal-Testing'
                            : 'Android/zimage/ZImage-multibranch/main'
                    copyArtifacts(
                            projectName: artifactJob,
                            selector: lastSuccessful()
                    )
                    if (fileExists('app/build/outputs/bundle/release/app-release.aab')) {
                        sh 'mv app/build/outputs/bundle/release/app-release.aab app-release.aab'
                    }
                }
            }
        }

        stage("Release to Google Play") {
            when { expression { ENVIRONMENT == 'develop' } }
            steps {
                script {

                    sh 'ls -l'
                    androidApkUpload(
                            googleCredentialsId: 'google-play-console-developer',
                            trackName:  'internal',
                            rolloutPercentage: '100',
                            filesPattern: "*.aab",
                            release_status: 'draft'
                    )
                    archiveArtifacts('app-release.aab')
                }
            }
        }
    }
    post {
        always {
            script {
                String appName = REPOSITORY.collect({ it.capitalize() }).join('-')
                new NotificationService(this).sendDiscordNotification(
                        "Jenkins Push to ${ENVIRONMENT.toLowerCase().capitalize()} for ${appName}: ${currentBuild.currentResult}",
                        env.BUILD_URL,
                        currentBuild.currentResult,
                        "Spring Build",
                        NotificationChannel.DISCORD_CICD
                )
            }
        }
    }
}
