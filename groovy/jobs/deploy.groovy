
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git',
                url: "git@github.com:zevrant/${REPOSITORY}.git"
    }

    stage("Deploy") {
        sh "VERSION=$VERSION envsubst < deployment.yml | kubectl --insecure-skip-tls-verify apply -n zevrant-home-services-$ENVIRONMENT -f -"
    }
}