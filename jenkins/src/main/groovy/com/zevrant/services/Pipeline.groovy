package com.zevrant.services

class Pipeline {

    String name;
    String description;
    PipelineParameter[] parameters;
    String gitRepo;
    String jenkisfileLocation;
    String credentialId;

    Pipeline(String name, String description, PipelineParameter[] parameters, String gitRepo, String jenkisfileLocation,
    credentialId) {
        this.name = name
        this.description = description
        this.parameters = parameters
        this.gitRepo = gitRepo
        this.jenkisfileLocation = jenkisfileLocation
        this.credentialId = credentialId
    }
}
