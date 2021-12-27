import com.lesfurets.jenkins.unit.global.lib.Library

@Library("CommonUtils") _

pipeline {
    agent {
        kubernetes {
            inheritFrom ""
        }
    }

    stages {

        stage("Get Artifact") {
            String artifactJob = (ENVIRONMENT == 'prod')
                    ? 'Android/Zevrant Android App/Zevrant-Android-App-Release-To-Internal-Testing'
                    : 'Android/Zevrant Android App/zevrant-android-app-multibranch/master'
            copyArtifacts(
                    projectName: artifactJob,
                    selector: lastSuccessful(),
                    filter: "app-release.aab"
            )
        }

        stage("Release to Google Play") {
            when { expression { ENVIRONMENT == 'develop' } }
            steps {
                script {

                    sh 'ls -l'
                    androidApkUpload(
                            googleCredentialsId: 'Zevrant Services',
                            trackName: (ENVIRONMENT == "prod")? 'production' : 'internal',
                            rolloutPercentage: '100',
                            filesPattern: "app/build/outputs/bundle/release/app-release.aab"
                    )
                    archiveArtifacts('app-release.aab')
                }
            }
        }
    }
}
