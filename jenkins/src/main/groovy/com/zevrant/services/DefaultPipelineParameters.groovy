package com.zevrant.services

enum DefaultPipelineParameters {
    REPOSITORY_PARAMETER(new PipelineParameter<String>("BRANCH_NAME", "Branch to be built", "")),
    BRANCH_PARAMETER(new PipelineParameter<String>("REPOSITORY", "Git repository to be built", ""));

    PipelineParameter parameter;

    private DefaultPipelineParameters(PipelineParameter parameter) {
        this.parameter = parameter
    }

    PipelineParameter getParameter() {
        return parameter
    }

}