node {
    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: "main",
                url: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-automation-tests.git"
    }

    stage("test") {
        sh "./gradlew clean test aggregate -Denvironment=develop --continue"
    }
}
