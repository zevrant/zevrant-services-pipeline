import groovy.json.JsonSlurper

@Library('CommonUtils') _

node {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    List<String> libraryRepositories = new ArrayList<>();
    stage("Get library Repositories") {
        libraryRepositories.addAll(getNonArchivedReposMatching("common"))
    }

    List<String> microserviceRepositories = new ArrayList<>();
    stage("Get Microservice Repositories") {
        microserviceRepositories.addAll(getNonArchivedReposMatching('service'))
        microserviceRepositories.addAll(getNonArchivedReposMatching('ui'))
        echo "Found these repositories ${microserviceRepositories}"
    }

    stage("Process Seed File") {
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