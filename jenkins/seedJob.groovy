@Library('CommonUtils')_

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    List<String> libraryRepositories = new ArrayList<>();
    stage("Get library Repositories") {

    }

    List<String> microserviceRepositories = new ArrayList<>();
    stage("Get Microservice Repositories") {

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
                        libraryRepositories: libraryRepositories,
                        microserviceRepositories: microserviceRepositories
                ]
        )
    }
}