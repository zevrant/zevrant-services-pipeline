package com.zevrant.services

enum DefaultPipelineParameters {
    REPOSITORY_PARAMETER(new PipelineParameter<String>(String.class, "BRANCH_NAME", "Branch to be built", "")),
    BRANCH_PARAMETER(new PipelineParameter<String>(String.class, "REPOSITORY", "Git repository to be built", ""));

    PipelineParameter parameter;

    private DefaultPipelineParameters(PipelineParameter parameter) {
        this.parameter = parameter
    }

    PipelineParameter getParameter() {
        return parameter
    }

}