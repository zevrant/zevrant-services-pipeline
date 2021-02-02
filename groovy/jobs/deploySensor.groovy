node {
    currentBuild.displayName = "Deploying $REPOSITORY version $VERSION"

    withCredentials([sshUserPrivateKey(credentialsId: 'jenkins-git', keyFileVariable: 'key', passphraseVariable: 'password', usernameVariable: 'username')]) {
        // some block

        def sensorLocations = ["192.168.1.20"];
        def baseSSHCommand = "ssh -i $key zevrant-sensor-service@";
        for(def sensorLocation : sensorLocations) {
            stage("Download Artifact") {
                sh "${baseSSHCommand}${sensorLocation} 'aws s3 cp s3://zevrant-artifact-store/com/zevrant/services/${REPOSITORY}/${version}/${REPOSITORY}-${version}.jar .'"
                sh "${baseSSHCommand}${sensorLocation} 'ln -sf ${REPOSITORY}-${version}.jar ${REPOSITORY}.jar"
            }

            stage("Start Service") {

            }
        }
    }
}