package com.zevrant.services.services

void checkout(String hostname = 'ssh://git@ssh.gitea.zevrant-services.com:30121',
        String org = 'zevrant-services',
        String repository,
        String branch = 'master',
        String credentialsId = 'jenkins-git') {
    checkout(
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
