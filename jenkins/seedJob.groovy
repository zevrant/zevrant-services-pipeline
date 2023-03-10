@Library('CommonUtils') _


import com.zevrant.services.pojo.LibraryCodeUnitCollection
import com.zevrant.services.pojo.SpringCodeUnitCollection

pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
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
                            lookupStrategy: 'SEED_JOB',
                            failOnMissingPlugin: true,
                            additionalClasspath: 'jenkins/src/main/groovy/', //only works with
                            additionalParameters: [
                                    libraryCodeUnits: LibraryCodeUnitCollection.libraries,
                                    springCodeUnits : SpringCodeUnitCollection.microservices
                            ]
                    )
                }
            }
        }
    }
}
