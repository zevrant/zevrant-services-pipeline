node {
    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: "master",
                url: "ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/zevrant-automation-tests.git"
    }

    stage("test") {
        sh "./gradlew clean test aggregate -Denvironment=develop --continue"
    }
}
