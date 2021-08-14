package com.zevrant.services;

class GitHubReleaseRequest {
    private final String tag_name;
    private final boolean prerelease;
    private final boolean draft;
    private final String name;

    GitHubReleaseRequest(String tag_name, boolean prerelease, boolean draft, String name) {
        this.tag_name = tag_name;
        this.prerelease = prerelease;
        this.draft = draft;
        this.name = name;
    }

    String getTagName() {
        return tag_name;
    }

    boolean isPrerelease() {
        return prerelease;
    }

    boolean isDraft() {
        return draft;
    }

    String getName() {
        return name;
    }
}
