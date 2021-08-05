package com.zevrant.services

class Pipeline {

    String name;
    String description;
    ArrayList<PipelineParameter> parameters;
    String gitRepo;
    String jenkinsfileLocation;
    String credentialId;

    Pipeline(Map<String, Object> params) {
        this.name = params.name
        this.description = params.description
        this.parameters = params.parameters as ArrayList<PipelineParameter> ?: new ArrayList<>();
        this.gitRepo = params.gitRepo ?: "git@github.com:Zevrant/zevrant-services-pipeline.git"
        this.jenkinsfileLocation = params.jenkinsfileLocation
        this.credentialId = params.credentialId
    }
}
