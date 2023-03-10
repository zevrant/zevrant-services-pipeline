@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.pojo.SpringCodeUnitCollection
import com.zevrant.services.pojo.LibraryCodeUnitCollection

List<String> libraryRepositories = new ArrayList<>();
List<String> microserviceRepositories = new ArrayList<>();
List giteaRepoList = null
List<String> ignoreRepos = ['zevrant-sensor-service', 'zevrant-security-service']
GitService gitService = ServiceLoader.load(binding, GitService.class) as GitService
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
                    jobDsl(
                            targets: 'jenkins/seed.groovy',
                            removedJobAction: 'DELETE',
                            removedViewAction: 'DELETE',
                            removedConfigFilesAction: 'DELETE',
                            lookupStrategy: 'SEED_JOB',
                            failOnMissingPlugin: true,
                            additionalClasspath: 'jenkins/src/main/groovy/', //only works with
                            additionalParameters: [
                                    libraryRepositories     : LibraryCodeUnitCollection.libraries,
                                    microserviceRepositories: SpringCodeUnitCollection.microservices
                            ]
                    )
                }
            }
        }
    }
}