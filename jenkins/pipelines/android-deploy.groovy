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
            copyArtifacts(
                    projectName: 'Android/Zevrant Android App/zevrant-android-app-multibranch/master',
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
                }
            }
        }
    }
}
