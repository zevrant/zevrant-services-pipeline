import groovy.json.JsonSlurper

node {

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
        sh "scp ./docker-compose.yml zevrant@192.168.0.150:/home/zevrant/docker-compose.yml"
        sh "ssh zevrant@192.168.0.150 'sudo rm /root/docker-compose.yml; sudo mv docker-compose.yml /root/docker-compose.yml"
        sh "ssh zevrant@192.168.0.150 'sudo docker-compose -f /root/docker-compose.yml up -d"

    }

    stage ("Version Update") {
        def splitVersion = version.tokenize(".");
        def minorVersion = splitVersion[2]
        minorVersion = minorVersion.toInteger() + 1

        sh "aws ssm put-parameter --name zevrant-proxy-service-VERSION --value ${splitVersion[0]}.${splitVersion[1]}.${minorVersion} --type String --overwrite"
    }
}