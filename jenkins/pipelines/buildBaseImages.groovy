import java.util.List

def imagesToBuild = ["zevrant-ubuntu-base"]
def branchesToBuild = ["develop"]

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

    Map<String, List<String>> affectedRepos = new HashMap<>();
    branchesToBuild.each({ branch ->
        affectedRepos.put(branch, []);
    })
    stage("Get Potential Repos") {
        def response = httpRequest authentication: 'jenkins-git-access-token', url: "https://api.github.com/orgs/zevrant/repos?type=all"
        List jsonResponse = readJSON text: response.content
        jsonResponse.each { repo ->
                    if ((repo['name'] as String).contains('zevrant')
                            && repo['name'] as String != 'zevrant-services-pipeline'
                            && !(repo['archived'] as Boolean)) {
                        branchesToBuild.each({branch ->
                            def dockerfileResponse = httpRequest(authentication: 'jenkins-git-access-token',
                                    contentType: "TEXT_PLAIN",
                                    validResponseCodes: "200:404",
                                    url: "https://raw.githubusercontent.com/zevrant/${repo['name'] as String}/master/Dockerfile")
                            if(dockerfileResponse.status < 400) {
                                for (image in imagesToBuild) {
                                    if(dockerfileResponse.content.contains(image)) {
                                        affectedRepos.get(branch).add(repo['name'] as String);
                                        break;
                                    }
                                }
                            }
                        })


                    }
                }
    }

    stage("Rebuild Downstream Repos") {
        def buildJobs = [:];
        branchesToBuild.each { branch ->
            println ("Build $branch branch for these repositories")

            buildJobs["Build $branch for ${affectedRepos.get(branch)}"] = {
                stage("Build $branch for ${affectedRepos.get(branch)}") {
                    build job: 'Build', parameters: [
                            [$class: 'StringParameterValue', name: 'REPOSITORY', value: affectedRepos.get(branch)],
                            [$class: 'StringParameterValue', name: 'BASE_BRANCH', value: "refs/heads/$branch"]
                    ]
                }
            }
        }
        parallel buildJobs
    }
}