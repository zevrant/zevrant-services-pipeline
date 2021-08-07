@Library('CommonUtils')_

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    stage("Process Seed File") {
        jobDsl(
                script: 'jenkins/seed.groovy',
                removeActions: 'DELETE',
                removedJobAction: 'DELETE',
                removedViewAction: 'DELETE',
                removedConfigFilesAction: 'DELETE',
                lookupStrategy: 'SEED_JOB',
                additionalClasspath: 'jenkins/src/main/groovy/',
                sandbox: true
        )
    }
}

static String processLibraryCode(String libraryCode) {
    int index = libraryCode.indexOf("class");
    index = (libraryCode.indexOf("enum") > 0 )? libraryCode.indexOf("enum") : index
    return libraryCode.substring(index);
}