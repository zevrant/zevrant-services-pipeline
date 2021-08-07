@Library('CommonUtils')_

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    String script = "";

    stage("Assemble Seed File") {
//        script += processLibraryCode(readFile("jenkins/src/main/groovy/com/zevrant/services/PipelineCollection.groovy"))
//        script += processLibraryCode(readFile("jenkins/src/main/groovy/com/zevrant/services/Pipeline.groovy"))
//        script += processLibraryCode(readFile("jenkins/src/main/groovy/com/zevrant/services/PipelineParameter.groovy"))
//        script += processLibraryCode(readFile("jenkins/src/main/groovy/com/zevrant/services/DefaultPipelineParameters.groovy"))
        script += "\n" + readFile("jenkins/seed.groovy")
    }

    stage("Process Seed File") {
        jobDsl(
                scriptText: script,
                removeActions: 'DELETE',
                removedJobAction: 'DELETE',
                removedViewAction: 'DELETE',
                removedConfigFilesAction: 'DELETE',
                lookupStrategy: 'SEED_JOB',
                additionalClassPath: 'jenkins/src/main/groovy/'
        )
    }
}

static String processLibraryCode(String libraryCode) {
    int index = libraryCode.indexOf("class");
    index = (libraryCode.indexOf("enum") > 0 )? libraryCode.indexOf("enum") : index
    return libraryCode.substring(index);
}