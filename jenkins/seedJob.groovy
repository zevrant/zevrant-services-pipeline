@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.ServiceLoader
import com.zevrant.services.pojo.SpringMicroserviceCollection

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
                                    libraryRepositories     : LibraryCodeUnitCollection.libraries,
                                    microserviceRepositories: SpringMicroserviceCollection.microservices
                            ]
                    )
                }
            }
        }
    }
}

List<String> getNonArchivedReposMatching(String searchTerm, List repoList) {
    List<String> matchingRepos = new ArrayList<>();

    repoList.each { repo ->
        String repoName = (repo['name'] as String).toLowerCase();
        if ((repoName as String).contains('zevrant')
                && (repoName as String).contains(searchTerm.toLowerCase())
                && !(repo['archived'] as Boolean)
                && (repoName as String) != 'zevrant-services-pipeline') {
            matchingRepos.add(repoName)
        }
    }
    return matchingRepos;
}

String getParameterValue(String parameter) {
    def json = readJSON text: (sh(returnStdout: true, script: "aws ssm get-parameter --name parameter"))
    return json['Parameter']['Value']
}
