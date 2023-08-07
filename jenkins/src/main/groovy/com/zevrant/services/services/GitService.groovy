package com.zevrant.services.services

class GitService extends Service {


    GitService(Object pipelineContext) {
        super(pipelineContext)
    }

    void checkout(String hostname = 'ssh://git@gitea.zevrant-services.com:30121',
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
                                                    url          : "${hostname}/${org}/${repository}.git"
                                            ]]
                ]
        )
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