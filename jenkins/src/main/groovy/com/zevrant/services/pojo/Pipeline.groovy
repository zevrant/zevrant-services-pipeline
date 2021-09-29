package com.zevrant.services.pojo

class Pipeline {

    private final String name;
    private final String description;
    private final ArrayList<PipelineParameter> parameters;
    private final String gitRepo;
    private final String jenkinsfileLocation;
    private final String credentialId;
    private final List<PipelineTrigger> triggers;
    private final int buildsToKeep;
    private final disabled;
    private final Map<String, String> envs;

    Pipeline(Map<String, Object> params) {
        this.name = params.name
        this.description = params.description ?: ""
        this.parameters = (params.parameters ?: new ArrayList<>()) as ArrayList<PipelineParameter>
        this.gitRepo = params.gitRepo ?: "git@github.com:Zevrant/zevrant-services-pipeline.git"
        this.jenkinsfileLocation = params.jenkinsfileLocation
        this.credentialId = params.credentialId
        this.triggers = (params.triggers as List<PipelineTrigger> ?: new ArrayList<>()) as List<PipelineTrigger>
        this.buildsToKeep = (params.buildsToKeep ?: 10) as int
        this.disabled = (params.disabled ?: false) as boolean
        this.envs = params.envs ? params.envs as Map<String, String> : null
    }

    String getName() {
        return name
    }

    String getDescription() {
        return description
    }

    ArrayList<PipelineParameter> getParameters() {
        return parameters
    }

    String getGitRepo() {
        return gitRepo
    }

    String getJenkinsfileLocation() {
        return jenkinsfileLocation
    }

    String getCredentialId() {
        return credentialId
    }

    List<PipelineTrigger> getTriggers() {
        return triggers
    }

    int getBuildsToKeep() {
        return buildsToKeep
    }

    def getDisabled() {
        return disabled
    }
}

