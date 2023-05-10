
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: "master",
                url: "ssh://git@gitea.zevrant-services.com:30121/zevrant-services/zevrant-automation-tests.git"
    }

    stage("Test") {
        try{
            sh "./gradlew clean test aggregate -Denvironment=develop -Dspring.profiles.active=develop --no-daemon"
        } finally {
            publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: false,
                    keepAll: false,
                    reportDir: 'target/site/serenity',
                    reportFiles: 'index.html',
                    reportName: 'Serenity Report',
                    reportTitles: ''
            ])
        }
    }

}