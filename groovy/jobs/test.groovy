
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: "master",
                url: "git@github.com:zevrant/zevrant-automation-tests.git"
    }

    stage("Test") {
        sh "./gradlew clean test aggregate -Denvironment=develop -Dspring.profiles.active=develop --no-daemon"
    }

    stage("publish") {
        publishHTML(target: [
                reportName : 'Serenity',
                reportDir:   'target/site/serenity',
                reportFiles: 'index.html',
                keepAll:     true,
                alwaysLinkToLastBuild: true,
                allowMissing: false
        ])
    }
}