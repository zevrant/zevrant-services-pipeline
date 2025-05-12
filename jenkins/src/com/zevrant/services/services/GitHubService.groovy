package com.zevrant.services.services

class GitHubService extends Service {

    private String gitHubToken;

    GitHubService(Object pipelineContext) {
        super(pipelineContext)
    }

    void setGitHubToken(String gitHubToken) {
        this.gitHubToken = gitHubToken
    }

    String getLatestRelease(String repoOwner, String repo, boolean includeToken = false) {
        List<Map<String, String>> customHeaders = [
                [
                        name : 'Content-Type',
                        value: 'application/vnd.github+json',
                ],
                [
                        name : 'X-GitHub-Api-Version',
                        value: '2022-11-28'
                ]
        ]

        if (includeToken) {
            ": Bearer $GITHUB_TOKEN"
            customHeaders.add(
                    [
                            name : 'Authorization',
                            value: 'Bearer ' + this.gitHubToken
                    ]
            )

        }

        def response = pipelineContext.httpRequest(
                method: 'GET',
                url: "https://api.github.com/repos/${repoOwner}/${repo}/releases/latest",
                consoleLogResponseBody: true,
                customHeaders: customHeaders
        )

        return response.content
    }

    String getDownloadUrlFromAssetsResponse(String assetsResponse) {
        def assetsResponseObject = pipelineContext.readJSON(text: assetsResponse)
        return assetsResponseObject.tag_name.replace('v', '')
    }
}
