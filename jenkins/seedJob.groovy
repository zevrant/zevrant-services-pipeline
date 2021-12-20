import groovy.json.JsonSlurper

@Library('CommonUtils') _

List<String> libraryRepositories = new ArrayList<>();
List<String> microserviceRepositories = new ArrayList<>();

pipeline{
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }
    stages {

        stage("Git Checkout") {
            steps {
                script {
                    git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
                }
            }
        }

        stage("Get library Repositories") {
            steps {
                script {
                    libraryRepositories.addAll(getNonArchivedReposMatching("common"))
                }
            }
        }

        stage("Get Microservice Repositories") {
            steps {
                script {
                    microserviceRepositories.addAll(getNonArchivedReposMatching('service'))
                    microserviceRepositories.addAll(getNonArchivedReposMatching('ui'))
                    microserviceRepositories.addAll(getNonArchivedReposMatching('backend'))

                    echo "Found these repositories ${microserviceRepositories}"
                }
            }
        }

        stage("Process Seed File") {
            steps {
                script {
                    jobDsl(
                            targets: 'jenkins/seed.groovy',
                            removeActions: 'DELETE',
                            removedJobAction: 'DELETE',
                            removedViewAction: 'DELETE',
                            removedConfigFilesAction: 'DELETE',
                            lookupStrategy: 'SEED_JOB',
                            additionalClasspath: 'jenkins/src/main/groovy/',
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

List<String> getNonArchivedReposMatching(String searchTerm) {
    List<String> matchingRepos = new ArrayList<>();
    def response = httpRequest authentication: 'jenkins-git-access-token', url: "https://api.github.com/orgs/zevrant/repos?type=all"
    List jsonResponse = readJSON text: response.content
    jsonResponse.each { repo ->
        String repoName = (repo['name'] as String).toLowerCase();
        if ((repoName as String).contains('zevrant')
                && (repoName as String).contains(searchTerm.toLowerCase())
                && !(repo['archived'] as Boolean)) {
            matchingRepos.add(repoName)
        }
    }
    return matchingRepos;
}

String getParameterValue(String parameter) {
    def json = readJSON text: (sh(returnStdout: true, script: "aws ssm get-parameter --name parameter"))
    return json['Parameter']['Value']
}
