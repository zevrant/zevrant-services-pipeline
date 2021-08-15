@Library('CommonUtils') _

import com.zevrant.services.GitHubReleaseRequest

node("master") {

    BASE_BRANCH = BASE_BRANCH.tokenize("/")
    BASE_BRANCH = BASE_BRANCH[BASE_BRANCH.size() - 1];
    currentBuild.displayName = "$REPOSITORY merging to $BASE_BRANCH"
    String version = "";
    String variant = ((BASE_BRANCH == "master") ? "release" : BASE_BRANCH) as String
    println(BRANCH_NAME + " " + variant)
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
        sh "keytool -v -importkeystore -srckeystore zevrant-services.p12 -srcstoretype PKCS12 -destkeystore zevrant-services.jks -deststoretype JKS -srcstorepass \'$password\' -deststorepass \'$password\' -noprompt"
        sh "zipalign -p -f -v 4 app/build/outputs/apk/$variant/app-$variant-unsigned.apk zevrant-services-unsigned.apk"
        sh "apksigner sign --ks zevrant-services.jks --in ./zevrant-services-unsigned.apk --out ./zevrant-services.apk --ks-pass \'pass:$password\'"
        sh "apksigner verify -v zevrant-services.apk"
    }

    stage("Release") {
        sh "aws s3 cp ./zevrant-services.apk s3://zevrant-apk-store/$variant/$version/zevrant-services.apk"
    }
}