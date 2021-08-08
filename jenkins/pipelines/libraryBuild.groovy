import groovy.json.JsonSlurper

node("master") {

    String repository = env.JOB_BASE_NAME;

    stage("Get Version") {
        def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name ${repository}-VERSION";
        JsonSlurper slurper = new JsonSlurper();
        Map parsedJson = slurper.parseText(jsonString) as Map;
        Map parameter = parsedJson.get("Parameter") as Map;
        version = parameter.get("Value");
    }

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: env.BRANCH_NAME,
                url: "git@github.com:zevrant/${repository}.git"
    }

    stage("Test") {
        "bash gradlew clean build --no-daemon"
    }

    stage("Version Update") {
        String[] splitVersion = version.tokenize(".");
        def minorVersion = splitVersion[2]
        minorVersion = minorVersion.toInteger() + 1
        version = "${splitVersion[0]}.${splitVersion[1]}.${minorVersion}"
        sh "aws ssm put-parameter --name ${repository}-VERSION --value $version --type String --overwrite"
    }

    stage("Build & Publish") {
        sh "bash gradlew clean assemble publish -PprojVersion=${version} --no-daemon"
    }

//    stage ("Deploy Library") {
//        build job: 'deploy-library', parameters: [
//                [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
//                [$class: 'StringParameterValue', name: 'VERSION', value: version],
//                [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
//        ]
//    }
}