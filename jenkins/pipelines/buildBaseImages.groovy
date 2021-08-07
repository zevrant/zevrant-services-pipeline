
def imagesToBuild = ["zevrant-ubuntu-base"];

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    stage("Build & Push Dockerfiles") {
        dir("docker/dockerfile") {
            imagesToBuild.each { image ->
                sh "docker build . -t zevrant/${image}:latest -f ${image}.dockerfile --no-cache --pull"
                sh "docker push zevrant/${image}:latest"
            }
        }
    }
}