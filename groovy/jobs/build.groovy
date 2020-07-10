import groovy.json.JsonSlurper

def angularProjects = ["zevrant-home-ui"];
def environments = ["develop", "prod"];
node {
    if(BASE_BRANCH == "develop") {

        stage("SCM Checkout") {
            git credentialsId: 'jenkins-git',
                    url: "git@github.com:zevrant/${REPOSITORY}.git"
        }

        stage ("Test"){
            if (angularProjects.indexOf(REPOSITORY) > 2) {
                sh "npm run test"
            } else {
                "bash gradlew clean build"
            }
        }

        def version;
        stage("Get Version") {
            def jsonString = sh returnStdout: true, script: "aws ssm get-parameter --name ${REPOSITORY}-VERSION";
            JsonSlurper slurper = new JsonSlurper();
            Map parsedJson = slurper.parseText(jsonString);
            Map parameter = parsedJson.get("Parameter");
            version = parameter.get("Value");
            print version

        }

        stage ("Build Artifact") {
            sh "bash gradlew clean assemble"
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

        stage ("Version Update") {
            def splitVersion = version.tokenize(".");
            print splitVersion;
            def minorVersion = splitVersion[2]
            minorVersion = minorVersion.toInteger() + 1

            sh "aws ssm put-parameter --name ${REPOSITORY}-VERSION --value ${splitVersion[0]}.${splitVersion[1]}.${minorVersion} --type String --overwrite"
        }

    }

}