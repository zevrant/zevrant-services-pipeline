package com.zevrant.services.pojo

import java.util.function.BiFunction

class GitHubArtifactMapping {

    private final String gitHubRepo
    private final String gitHubRepoOwner
    private final BiFunction<String, Object, String> responseParserFunction

    GitHubArtifactMapping(String gitHubRepo, String gitHubRepoOwner) {
        this.gitHubRepo = gitHubRepo
        this.gitHubRepoOwner = gitHubRepoOwner
        this.responseParserFunction = responseParserFunction
    }

    String getGitHubRepo() {
        return gitHubRepo
    }

    String getGitHubRepoOwner() {
        return gitHubRepoOwner
    }
}
