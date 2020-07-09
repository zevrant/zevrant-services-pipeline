
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git',
                url: "git@github.com:zevrant/${REPOSITORY}.git"
    }

    stage("Deploy") {
        sh "VERSION=$VERSION envsubst < deployment.yml | kubectl apply -n zevrant-home-services-prod -f -"
    }
}