package com.zevrant.services;

public class GitHubReleaseRequest {
    private final String tag_name;
    private final boolean prerelease;
    private final boolean draft;
    private final String name;

    public GitHubReleaseRequest(String tag_name, boolean prerelease, boolean draft, String name) {
        this.tag_name = tag_name;
        this.prerelease = prerelease;
        this.draft = draft;
        this.name = name;
    }

    public String getTag_name() {
        return tag_name;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public boolean isDraft() {
        return draft;
    }

    public String getName() {
        return name;
    }
}
