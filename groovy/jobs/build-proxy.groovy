node {
    if(ACTION != "closed") {
        currentBuild.result = 'ABORTED'
        error('PR not closed')
    }

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git',
                url: "git@github.com:zevrant/zevrant-proxy-service.git"
    }

    stage ("Test"){
        "bash gradlew clean build"
    }

    def version;
    stage("Get Version") {
        def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name zevrant-proxy-service-VERSION";
        JsonSlurper slurper = new JsonSlurper();
        Map parsedJson = slurper.parseText(jsonString);
        Map parameter = parsedJson.get("Parameter");
        version = parameter.get("Value");
        print version

    }

    stage ("Build Artifact") {
        sh "bash gradlew clean assemble"
        sh "docker build -t zevrant/zevrant-proxy-service:$version ."
        sh "docker push zevrant/zevrant-proxy-service:$version"
    }

    stage ("Deploy") {

    }

    stage ("Version Update") {
        def splitVersion = version.tokenize(".");
        def minorVersion = splitVersion[2]
        minorVersion = minorVersion.toInteger() + 1

        sh "aws ssm put-parameter --name zevrant-proxy-service-VERSION --value ${splitVersion[0]}.${splitVersion[1]}.${minorVersion} --type String --overwrite"
    }
}