@Library('CommonUtils')

import com.zevrant.services.PipelineCollection

import java.util.stream.Collectors

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    String script = "";

    stage("Assemble Seec File") {
        script += processLibraryCode(readFile("jenkins/src/main/groovy/com/zevrant/services/Pipeline.groovy"))
        println script
        script += "\n" + readFile("jenkins/seed.groocy")
    }

    stage("Process Seed File") {
        jobDsl(
                scriptText: script,
                removeActions: 'DELETE',
                removedJobAction: 'DELETE',
                removedViewAction: 'DELETE',
                removedConfigFilesAction: 'DELETE',
                lookupStrategy: 'SEED_JOB',
                additionalClassPath: 'src'
        )
    }
}

static String processLibraryCode(String libraryCode) {
    return Arrays.asList(libraryCode.split('\n')).stream().filter(line -> {
        return line.contains("package com.zevrant.services")
    }).collect(Collectors.toList()).join("\n");
}