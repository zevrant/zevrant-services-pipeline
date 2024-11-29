package com.zevrant.services.services

import Version
import SpringCodeUnit

class GradleService extends Service {

    private final MinioService minioService

    GradleService(Object pipelineContext) {
        super(pipelineContext)
        minioService = new MinioService(pipelineContext)
    }

    void unitTest() {
        pipelineContext.sh 'bash gradlew test'
    }

    void integrationTest() {
        pipelineContext.sh 'bash gradlew integrationTest'
    }

    void assemble(Version version) {
        pipelineContext.sh "bash gradlew assemble -PprojVersion=${version.toVersionCodeString()}"
    }

    void publish(Version version, SpringCodeUnit springCodeUnit) {
        pipelineContext.sh "bash gradlew publish -PprojVersion=${version.toVersionCodeString()}"

    }

}