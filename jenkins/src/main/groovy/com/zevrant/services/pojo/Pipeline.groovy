package com.zevrant.services.pojo

class Pipeline {

    private final String name;
    private final String folder
    private final String description;
    private final ArrayList<PipelineParameter> parameters;
    private final String gitRepo;
    private final String jenkinsfileLocation;
    private final String credentialId;
    private final List<PipelineTrigger> triggers;
    private final int buildsToKeep;
    private final disabled;
    private final Map<String, String> envs
    private final boolean allowConcurrency

    Pipeline(Map<String, Object> params) {
        this.name = params.name
        this.displayName = (params.displayName)? params.displayName : params.name
        this.folder = params.folder ?: ''
        this.description = params.description ?: ""
        this.parameters = (params.parameters ?: new ArrayList<>()) as ArrayList<PipelineParameter>
        this.gitRepo = params.gitRepo ?: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git"
        this.jenkinsfileLocation = params.jenkinsfileLocation
        this.credentialId = params.credentialId ?: 'jenkins-git'
        this.triggers = (params.triggers as List<PipelineTrigger> ?: new ArrayList<>()) as List<PipelineTrigger>
        this.buildsToKeep = (params.buildsToKeep ?: 10) as int
        this.disabled = (params.disabled ?: false) as boolean
        this.envs = params.envs ? params.envs as Map<String, String> : new HashMap<>() as Map<String, String>
        this.allowConcurrency = params.allowConcurency
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

    boolean getDisabled() {
        return disabled
    }

    boolean getAllowConcurrency() {
        return allowConcurrency
    }

    Map<String, String> getEnvs() {
        return envs
    }

}

