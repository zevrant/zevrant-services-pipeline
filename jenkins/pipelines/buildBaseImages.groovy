
def imagesToBuild = ["zevrant-ubuntu-base"];

node("master") {
    dir("docker/dockerfile") {
        imagesToBuild.each { image ->
            sh "docker build -t zevrant/${image}:latest -f ${image}.dockerfile --no-cache"
            sh "docker push zevrant/${image}:latest"
        }
    }
}