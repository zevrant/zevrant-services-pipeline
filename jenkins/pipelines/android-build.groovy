@Library('CommonUtils') _

import com.zevrant.services.GitHubReleaseRequest

node("master") {

    BASE_BRANCH = BASE_BRANCH.tokenize("/")
    BASE_BRANCH = BASE_BRANCH[BASE_BRANCH.size() - 1];
    currentBuild.displayName = "$REPOSITORY merging to $BASE_BRANCH"
    String version = "";
    stage("Get Version") {
        def json = readJSON text: (sh(returnStdout: true, script: "aws ssm get-parameter --name ${repository}-VERSION"))
        version = json['Parameter']['Value']
    }


    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: BASE_BRANCH,
                url: "git@github.com:zevrant/${REPOSITORY}.git"
    }

    stage("Test") {
        "bash gradlew clean build --no-daemon"
    }

    stage("Version Update") {
        def splitVersion = version.tokenize(".");
        def minorVersion = splitVersion[2]
        minorVersion = minorVersion.toInteger() + 1
        version = "${splitVersion[0]}.${splitVersion[1]}.${minorVersion}"
        sh "aws ssm put-parameter --name ${REPOSITORY}-VERSION --value $version --type String --overwrite"
    }

    stage("Build Artifact") {
        String variant = (((BASE_BRANCH == "master") ? "release" : BASE_BRANCH) as String).capitalize()
        def json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/keystore"))
        String keystore = json['SecretString']; writeFile file: './zevrant-services.txt', text: keystore
        sh "base64 -d ./zevrant-services.txt > ./zevrant-services.p12"
        json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/password"))
        String password = json['SecretString']
        sh " SIGNING_KEYSTORE=\'${env.WORKSPACE}/zevrant-services.p12\'" + 'KEYSOTRE_PASSWORD=\'' + password + "\' bash gradlew clean assemble$variant --no-daemon --info"
    }

    stage("Release") {
        GitHubReleaseRequest request = new GitHubReleaseRequest(version, true, true, "Release v$version");
        String requestBodyJson = writeJSON returnText: true, json: request
        httpRequest(
                authentication: 'jenkins-git-access-token',
                url: "https://api.github.com/repos/zevrant/zevrant-android-app/releases",
                requestBody: requestBodyJson
        )
    }
}