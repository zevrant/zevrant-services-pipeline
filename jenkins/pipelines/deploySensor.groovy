import groovy.json.JsonSlurper


node {
    currentBuild.displayName = "Deploying $REPOSITORY version $VERSION"

    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-git', keyFileVariable: 'key', passphraseVariable: 'password', usernameVariable: 'username')]) {
        // some block

        String[] sensorLocations = null;
        String[] devSensorLocations = ["192.168.1.20"];
        String[] productionSensorLocations = [];

        if(ENVIRONMENT == "production") {
            sensorLocations = productionSensorLocations
        } else {
            sensorLocations = devSensorLocations
        }

        def stepsForParallel = [:];

        String baseSSHCommand = "ssh -i $key zevrant-sensor-service@" as String;
        for (String sensorLocation : sensorLocations) {
            stage("Download Artifact") {

                sh "${baseSSHCommand}${sensorLocation} 'set +e rm *-*.*.jar'"
                sh "${baseSSHCommand}${sensorLocation} 'aws s3 cp s3://zevrant-artifact-store/com/zevrant/services/${REPOSITORY}/${VERSION}/${REPOSITORY}-${VERSION}.jar .'"
                sh "${baseSSHCommand}${sensorLocation} 'ln -sf ${REPOSITORY}-${VERSION}.jar ${REPOSITORY}.jar'"

            }

            stage("Start Service") {
                sh "${baseSSHCommand}${sensorLocation} 'sudo /bin/systemctl restart zevrant-sensor-service.service'"
                sleep(180)
                for (def Integer i = 0; i < 10; i++) {
                    try {
                        def response = sh returnStdout: true, script: "http_proxy= && curl --insecure https://${sensorLocation}:9006/zevrant-sensor-service/actuator/health"

                        JsonSlurper slurper = new JsonSlurper();
                        Map parsedJson = slurper.parseText(response);
                        def status = parsedJson.get("status");
                        if(status.equals("UP")) {
                            break;
                        }
                    } catch (Exception ex) {
                        if(i == 9) {
                            throw ex;
                        }
                        sleep(15)
                    }
                }
            }
        }

    }
}