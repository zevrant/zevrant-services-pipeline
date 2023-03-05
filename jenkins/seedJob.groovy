@Library('CommonUtils') _

import com.zevrant.services.services.GitService
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.ServiceLoader

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

        stage("Git Checkout & List Repositories") {
            environment {
                GITEA_ACCESS_TOKEN = credentials('jenkins-git-access-token-as-text')
            }
            steps {
                script {
                    println "Services count = ${KubernetesServiceCollection.getServices().size()}"
                    gitService.checkout('zevrant-services-pipeline')
                    def response = httpRequest(
                            url: "https://gitea.zevrant-services.com/api/v1/orgs/zevrant-services/repos",
                            contentType: 'APPLICATION_JSON',
                            acceptType: 'APPLICATION_JSON',
                            httpMode: 'GET',
                            useSystemProperties: true,
                            ignoreSslErrors: true,
                            customHeaders: [
                                    [name: 'Authorization', value: 'token ' + env.GITEA_ACCESS_TOKEN]
                            ],
                    )

                    assert response != null: "Response for repo list was null"
                    assert response.content != null: "Response did not return a response body as expected"
                    giteaRepoList = readJSON text: response.content
                }
            }
        }

        stage("Check Repositories") {
            parallel {
                stage("Get library Repositories") {
                    steps {
                        script {
                            libraryRepositories.addAll(getNonArchivedReposMatching("common", giteaRepoList))
                        }
                    }
                }

                stage("Get Microservice Repositories") {
                    steps {
                        script {
                            microserviceRepositories.addAll(getNonArchivedReposMatching('service', giteaRepoList))
                            microserviceRepositories.addAll(getNonArchivedReposMatching('ui', giteaRepoList))
                            microserviceRepositories.addAll(getNonArchivedReposMatching('backend', giteaRepoList))

                            echo "Found these repositories ${microserviceRepositories}"

                            ignoreRepos.each( { repo ->
                                microserviceRepositories.removeAll(getNonArchivedReposMatching(repo, giteaRepoList))
                            })


                            echo "removed these repositories ${ignoreRepos}"
                        }
                    }
                }
            }
        }
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
                                    libraryRepositories     : libraryRepositories,
                                    microserviceRepositories: microserviceRepositories
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
