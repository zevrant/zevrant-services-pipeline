
node("master") {

    String ipAddress = "";

    stage("Get IP") {
        IP=`curl https://ipinfo.io/ip`

    }
}