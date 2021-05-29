import groovy.json.JsonSlurper

node {
    BASE_BRANCH = BASE_BRANCH.tokenize("/")
    BASE_BRANCH = BASE_BRANCH[BASE_BRANCH.size() - 1];

    def version;
    stage("Get Version") {
        def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name ${REPOSITORY}-VERSION";
        JsonSlurper slurper = new JsonSlurper();
        Map parsedJson = slurper.parseText(jsonString);
        Map parameter = parsedJson.get("Parameter");
        version = parameter.get("Value");
    }

    currentBuild.displayName = "$REPOSITORY merging to $BASE_BRANCH"

    if(BASE_BRANCH == "develop") {

        stage("SCM Checkout") {
            git credentialsId: 'jenkins-git', branch: BASE_BRANCH,
                    url: "git@github.com:zevrant/${REPOSITORY}.git"
        }

        stage ("Test"){
                "bash gradlew clean test"
        }

        stage ("Version Update") {
            def splitVersion = version.tokenize(".");
            def minorVersion = splitVersion[2]
            minorVersion = minorVersion.toInteger() + 1
            version = "${splitVersion[0]}.${splitVersion[1]}.${minorVersion}"
            sh "aws ssm put-parameter --name ${REPOSITORY}-VERSION --value $version --type String --overwrite"
        }

        stage ("Build Artifact") {
            sh "bash gradlew clean assemble -PprojVersion=$version"
            sh "aws s3 cp build/libs/${REPOSITORY}-${version}.jar s3://zevrant-artifact-store/com/zevrant/services/${REPOSITORY}/${version}/${REPOSITORY}-${version}.jar"
        }

        stage ("Deploy Sensor") {
            build job: 'Deploy-Sensor', parameters: [
                    [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
                    [$class: 'StringParameterValue', name: 'VERSION', value: version]
            ]
        }

    }
}