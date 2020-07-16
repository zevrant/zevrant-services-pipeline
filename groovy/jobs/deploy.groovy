import java.util.regex.Pattern;

node {

    def branch;
    if(ENVIRONMENT == 'prod') {
        branch = "master";
    } else{
        branch = ENVIRONMENT
    }

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: branch,
                url: "git@github.com:zevrant/${REPOSITORY}.git"
    }

    stage("Deploy Database") {
        if( fileExists('database.yml')) {
            print ENVIRONMENT
            sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./database.yml"
            sh "kubectl apply -n zevrant-home-services-$ENVIRONMENT -f ./database.yml"
            sh "kubectl rollout status deployments $REPOSITORY-db-deployment -n zevrant-home-services-$ENVIRONMENT"
        }
    }

    stage("Deploy") {
        sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./deployment.yml"
        sh "sed -i 's/\$VERSION/$VERSION/g' ./deployment.yml"
        sh "kubectl apply -n zevrant-home-services-$ENVIRONMENT -f ./deployment.yml"
        sh "kubectl rollout status deployments $REPOSITORY-deployment -n zevrant-home-services-$ENVIRONMENT"
    }
}