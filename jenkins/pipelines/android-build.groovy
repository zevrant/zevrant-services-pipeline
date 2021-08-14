@Library('CommonUtils') _

import com.zevrant.services.GitHubReleaseRequest

node("master") {

    BASE_BRANCH = BASE_BRANCH.tokenize("/")
    BASE_BRANCH = BASE_BRANCH[BASE_BRANCH.size() - 1];
    currentBuild.displayName = "$REPOSITORY merging to $BASE_BRANCH"
    String version = "";
    String variant = ((BASE_BRANCH == "master") ? "release" : BASE_BRANCH) as String
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
        def json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/keystore"))
        String keystore = json['SecretString']; writeFile file: './zevrant-services.txt', text: keystore
        sh "base64 -d ./zevrant-services.txt > ./zevrant-services.p12"
        json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/password"))
        String password = json['SecretString']
        sh " SIGNING_KEYSTORE=\'${env.WORKSPACE}/zevrant-services.p12\' " + 'KEYSTORE_PASSWORD=\'' + password + "\' bash gradlew clean assemble${variant.capitalize()} --no-daemon"
        //for some reason gradle isn't signing like it's suppost to so we do it manually
//        sh "keytool -v -importkeystore -srckeystore zevrant-services.p12 -srcstoretype PKCS12 -destkeystore zevrabt-services.jks -deststoretype JKS -srcstorepass \'$password\' -deststorepass \'$password\' -noprompt\n"
//        sh "zipalign -p -f -v 4 app/build/outputs/apk/$variant/app-$variant-unsigned.apk ./zevrant-services-unsignedsigned.apk"
//        sh "apksigner verify -v zevrant-services-unsigned.apk --out zevrant-services.apk"
    }

    stage("Release") {
        GitHubReleaseRequest request = new GitHubReleaseRequest(version, true, true, "Release v$version");
        String requestBodyJson = writeJSON returnText: true, json: request
        def response = readJSON( text: httpRequest(
                authentication: 'jenkins-git-access-token',
                url: "https://api.github.com/repos/zevrant/zevrant-android-app/releases",
                requestBody: requestBodyJson,
                httpMode: 'POST'
        ).content)
        println "uploading to " + response['assets_url']
        withCredentials([usernamePassword(credentialsId: 'jenkins-git-access-token', passwordVariable: 'password', usernameVariable: 'username')]) {
            sh "curl -X 'POST' -I -H 'Authorization: token: $password' -H 'Content-Type: application/vnd.android.package-archive' -X POST --data-binary @'app/build/outputs/apk/release/app-release-unsigned.apk' ${response['assets_url']}?name=test.apk&label=test"
        }

    }
}