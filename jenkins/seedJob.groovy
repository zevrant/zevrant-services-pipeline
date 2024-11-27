@Library('CommonUtils') _


pipeline {
    agent {
        label 'master-node'
    }
    stages {

        stage("Process Seed File") {
            steps {
                script {
                    sh 'ls -l'
                    jobDsl(
                            targets: 'jenkins/seed.groovy',
                            removedJobAction: 'DELETE',
                            removedViewAction: 'DELETE',
                            removedConfigFilesAction: 'DELETE',
                            lookupStrategy: 'JENKINS_ROOT',
                            failOnMissingPlugin: true,
                            additionalClasspath: 'jenkins/src/main/groovy', //only works with
                            ignoreExisting: true,
                    )
                }
            }
        }
    }
}
