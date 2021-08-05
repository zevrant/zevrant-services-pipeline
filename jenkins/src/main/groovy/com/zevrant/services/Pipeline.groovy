package com.zevrant.services

class Pipeline {

    String name;
    String description;
    PipelineParameter[] parameters;
    String gitRepo;
    String jenkisfileLocation;
    String credentialId;

    Pipeline(Map<String, Object> params) {
        this.name = params.name
        this.description = params.description
        this.parameters = params.parameters as PipelineParameter[] ?: new ArrayList<>();
        this.gitRepo = params.gitRepo ?: "git@github.com:Zevrant/zevrant-services-pipeline.git"
        this.jenkisfileLocation = params.jenkisfileLocation
        this.credentialId = params.credentialId
    }
}
