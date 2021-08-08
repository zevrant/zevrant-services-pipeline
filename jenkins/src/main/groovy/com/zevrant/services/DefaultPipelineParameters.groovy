package com.zevrant.services

enum DefaultPipelineParameters {
    REPOSITORY_PARAMETER(new PipelineParameter<String>(String.class, "REPOSITORY", "Branch to be built", "")),
    BRANCH_PARAMETER(new PipelineParameter<String>(String.class, "BRANCH_NAME", "Git repository to be built", "master"));

    PipelineParameter parameter;

    private DefaultPipelineParameters(PipelineParameter parameter) {
        this.parameter = parameter
    }

    PipelineParameter getParameter() {
        return parameter
    }

}