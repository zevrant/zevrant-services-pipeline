import groovy.json.JsonSlurper
//def envVars = Jenkins.instance.getGlobalNodeProperties()[0].getEnvVars()
def BASE_BRANCH = System.getproperty('BASE_BRANCH')
def REPOSITORY = envVars['REPOSITORY']
def ACTION = envVars['ACTION']

def angularProjects = ["zevrant-home-ui"];
def environments = ["develop", "prod"];
node {
    sh "printenv"
    if($BASE_BRANCH == "develop") {

        stage("SCM Checkout") {
            git credentialsId: 'jenkins-git',
                    url: "git@github.com:zevrant/${REPOSITORY}.git"
        }

        stage ("Test"){
            if ($angularProjects.indexOf($REPOSITORY) > 2) {
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
            sh "docker build -t $REPOSITORY:$version ."
            sh "docker push zevrant/$REPOSITORY:$version"
        }

        stage ("Deploy") {
            build job: 'Deploy', parameters: [
                    [$class: 'StringParameterValue', name: 'REPOSITORY', value: $REPOSITORY],
                    [$class: 'StringParameterValue', name: 'VERSION', value: version],
                    [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
            ]
        }

    }

}