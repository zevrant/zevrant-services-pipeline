package com.zevrant.services;

class GitHubReleaseRequest {
    final String tag_name;
    final boolean prerelease;
    final boolean draft;
    final String name;

    GitHubReleaseRequest(String tag_name, boolean prerelease, boolean draft, String name) {
        this.tag_name = tag_name;
        this.prerelease = prerelease;
        this.draft = draft;
        this.name = name;
    }
}
