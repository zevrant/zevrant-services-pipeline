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

    List<String> affectedRepos = [];
    stage("Get Potential Repos") {
        def response = httpRequest authentication: 'jenkis-git-access-token', url: "https://api.github.com/orgs/zevrant/repos?type=all"
        List jsonResponse = readJSON text: response.content
        jsonResponse.stream()
                .each { repo ->
                    if ((repo['name'] as String).contains('zevrant')
                            && repo['name'] as String != 'zevrant-services-pipeline'
                            && !(repo['archived'] as Boolean)) {
                        def dockerfileResponse = httpRequest authentication: 'jenkins-git-access-token',
                                contentType: "TEXT_PLAIN"
                                url: "https://raw.githubusercontent.com/zevrant/${repo['name'] as String}/master/Dockerfile"
//                        if(dockerfileResponse.status < 400) {
//                            for (image in imagesToBuild) {
//                                if(dockerfileResponse.content.contains(image)) {
////                                    affectedRepos.add(repo['name'] as String);
//                                    break;
//                                }
//                            }
//                        }

                    }
                }
    }

    stage("Update Downstream Repos") {
        println "MicroServices to update ${affectedRepos}"
    }
}