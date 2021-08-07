import java.util.List

def imagesToBuild = ["zevrant-ubuntu-base"];

node("master") {

    stage("Git Checkout") {
        git(url: 'git@github.com:zevrant/zevrant-services-pipeline.git', credentialsId: 'jenkins-git', branch: 'master')
    }

    stage("Build & Push Dockerfiles") {
//        dir("docker/dockerfile") {
//            imagesToBuild.each { image ->
//                sh "docker build . -t zevrant/${image}:latest -f ${image}.dockerfile --no-cache --pull"
//                sh "docker push zevrant/${image}:latest"
//            }
//        }
    }

    stage("Update Downstream Repos") {
        def response = httpRequest authentication: 'jenkis-git-access-token', url: "https://api.github.com/orgs/zevrant/repos?type=all"
        List jsonResponse = readJSON text: response.content
        jsonResponse.stream()
                .each { repo ->
                    if ((repo['name'] as String).contains('zevrant')
                            && repo['name'] as String != 'zevrant-services-pipeline'
                            && !(repo['archived'] as Boolean)) {
                        println repo['name']
                    }
                }
    }
}