package com.zevrant.services.services

class Service {

    protected def pipelineContext

    Service(def pipelineContext) {
        this.pipelineContext = pipelineContext
    }
}