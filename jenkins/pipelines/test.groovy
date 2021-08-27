
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: "test",
                url: "git@github.com:zevrant/zevrant-services-pipeline.git"
    }

    stage("Test") {
        sh "kubectl apply -f kubernetes/dev/ssh-server.yml"
        sh "kubectl apply -f kubernetes/dev/ui-service.yml"
    }

}