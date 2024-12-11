package com.zevrant.services.services

class GitService extends Service {


    GitService(Object pipelineContext) {
        super(pipelineContext)
    }

    void checkout(String hostname = 'ssh://git@gitea.zevrant-services.internal:30121',
                  String org = 'zevrant-services',
                  String repository,
                  String branch = 'main',
                  String credentialsId = 'jenkins-git') {
        pipelineContext.checkout(
                scm: [
                        $class           : 'GitSCM',
                        branches         : [[
                                                    name: branch
                                            ]],
                        extensions       : [[$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true]],
                        userRemoteConfigs: [[
                                                    credentialsId: credentialsId,
                                                    url          : "${hostname}:${org}/${repository}.git"
                                            ]]
                ]
        )
    }

    List<String> getFilesChanged() {
        pipelineContext.sh ' git log | head -n 1'
        String changes = pipelineContext.sh(returnStdout: true, script: 'git diff-tree --no-commit-id --name-only "$(git log | head -n 1 | awk \'{print $2}\')" -r')
        pipelineContext.println(changes)
        return changes.split('\\h')
    }

    void tagVersion(String version, String credentialsId) {
        pipelineContext.sshagent(credentials: [credentialsId]) {
            pipelineContext.sh("git tag ${version}")
            pipelineContext.sh("git push origin ${version}")
        }
    }

    String getNextBetaTagForVersion(String artifactVersion) { //Provide an artifact version without the beta suffix to get the next beta version for, DO NOT COMMIT THIS BETA VERSION TO MONGO
        pipelineContext.sh("git tag -l | grep $artifactVersion-beta | tee tags")
        String tagsString = pipelineContext.readFile(file: 'tags')
        pipelineContext.sh 'rm tags'
        List<String> betaTags = Arrays.asList(tagsString.split("\\n"))
        String betaVersion = "${artifactVersion}-beta.1" //version we start with for consistent

        if ( !betaTags.isEmpty() ) {
            int highestBetaVersion = 1
            betaTags.each { tag ->
                String[] tagParts = tag.split("\\.")
                if (tagParts.length > 1) {
                    String betaVersionString = tagParts[tagParts.length - 1].trim()
                    try {
                        pipelineContext.println("Parsing beta version $betaVersionString")
                        int betaVersionInt = Integer.parseInt(betaVersionString)
                        pipelineContext.println("testing version $betaVersionInt against previous highest Version $highestBetaVersion")
                        highestBetaVersion = Math.max(betaVersionInt, highestBetaVersion)
                    } catch (NumberFormatException ex) {
                        pipelineContext.println("Failed to parse betaVersion number for tag $tag, skipping due to incorrect format")
                        pipelineContext.println("Exception message was ${ex.getMessage()}")
                        pipelineContext.println(ex.printStackTrace())
                    }
                }
            }
            betaVersion = "${artifactVersion}-beta.${highestBetaVersion + 1}"
        }
        return betaVersion
    }

    void cleanUntrackedFiles() {
        String untrackedFiles = pipelineContext.sh returnStdout: true, script: 'git ls-files --others --exclude-standard'
        untrackedFiles.split('\\h|\\n|\\r\\n').each {file ->
            if (!file.trim().isBlank()) {
                pipelineContext.println("Deleting ${file}")
                pipelineContext.sh "rm -rf ${file}"
            }
        }
    }

//oid postBuildPrHook(GitHubRepo gitHubRepo) {
//    GitHubRepo repo = new GitHubRepo(repoSsh)
//    BuildManagement buildManagement = TaskLoader.load(binding, BuildManagement)
//
//    String prNumber = buildManagement.getPrNumber()
//    String credentialsId = gitHubRepo.credentialsId
//
//    if (prNumber) {
//        Map<String, Object> prDetails = getPullRequestDetails(
//                credentialsId,
//                repo.organization,
//                repo.name,
//                prNumber
//        )
//        notifyPrReviewer(prDetails)
//        String color = 'danger'
//        String message = 'Build Failed.'
//
//        if (prDetails.draft) {
//            color = 'warning'
//            message = 'This build was cut short because the pull request is marked as draft. To complete a full ' +
//                    'build, run the Jenkins job again after marking this pull request as ready for review.'
//        }
//        notifyPrBuildResult(
//                prDetails, repo.organization, repo.name,
//                color,
//                message
//        )
//    } else {
//        echo 'Could not find a corresponding PR to notify from'
//    }
//}

}