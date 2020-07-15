import groovy.json.JsonSlurper

def angularProjects = ["zevrant-home-ui"];
def environments = ["develop", "prod"];
node {
    if(ACTION != "closed") {
        currentBuild.result = 'ABORTED'
        error('PR not closed')
    }
    if(BASE_BRANCH == "develop") {

        stage("SCM Checkout") {
            git credentialsId: 'jenkins-git', branch: BASE_BRANCH,
                    url: "git@github.com:zevrant/${REPOSITORY}.git"
        }

        stage ("Test"){
            if (angularProjects.indexOf(REPOSITORY) > 2) {
                sh "npm run test"
            } else {
                "bash gradlew clean build --no-daemon"
            }
        }

        def version;
        stage("Get Version") {
            def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name ${REPOSITORY}-VERSION";
            JsonSlurper slurper = new JsonSlurper();
            Map parsedJson = slurper.parseText(jsonString);
            Map parameter = parsedJson.get("Parameter");
            version = parameter.get("Value");
        }

        stage ("Version Update") {
            def splitVersion = version.tokenize(".");
            def minorVersion = splitVersion[2]
            minorVersion = minorVersion.toInteger() + 1
            version = "${splitVersion[0]}.${splitVersion[1]}.${minorVersion}"
            sh "aws ssm put-parameter --name ${REPOSITORY}-VERSION --value $version --type String --overwrite"
        }

        stage ("Build Artifact") {
            sh "bash gradlew clean assemble --no-daemon"
            sh "docker build -t zevrant/$REPOSITORY:$version ."
            sh "docker push zevrant/$REPOSITORY:$version"
        }

        stage ("Deploy") {
            build job: 'Deploy', parameters: [
                    [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
                    [$class: 'StringParameterValue', name: 'VERSION', value: version],
                    [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
            ]
        }

    }
    if(BASE_BRANCH == "master") {
        def version;
        stage("Get Version") {
            def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name ${REPOSITORY}-VERSION";
            JsonSlurper slurper = new JsonSlurper();
            Map parsedJson = slurper.parseText(jsonString);
            Map parameter = parsedJson.get("Parameter");
            version = parameter.get("Value");
            print version
        }

        def splitVersion = version.tokenize(".");
        def mainVersion = splitVersion[0]
        mainVersion = mainVersion.toInteger() + 1

        stage("Stage Release") {
            sh "aws ssm put-parameter --name ${REPOSITORY}-VERSION --value ${mainVersion}.0.0 --type String --overwrite"
            build job: 'Deploy', parameters: [
                    [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
                    [$class: 'StringParameterValue', name: 'VERSION', value: version],
                    [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "prod"]
            ]
        }
    }

}