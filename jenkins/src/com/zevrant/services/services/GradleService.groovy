package com.zevrant.services.services

import com.zevrant.services.pojo.Version
import com.zevrant.services.pojo.codeunit.SpringCodeUnit

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
        pipelineContext.sh "bash gradlew assemble -PprojVersion=${version.toThreeStageVersionString()}"
    }

    void publish(Version version, SpringCodeUnit springCodeUnit) {
        pipelineContext.sh "bash gradlew publish -PprojVersion=${version.toVersionCodeString()}"

    }

}