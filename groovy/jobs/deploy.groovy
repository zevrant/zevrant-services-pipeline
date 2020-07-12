
node {

    stage("SCM Checkout") {
        git credentialsId: 'jenkins-git', branch: ENVIRONMENT,
                url: "git@github.com:zevrant/${REPOSITORY}.git"
    }

    stage("Deploy Database") {
        if( fileExists('database.yml')) {
            sh "ENVIRONMENT=$ENVIRONMENT envsubst < database.yml | kubectl apply -n zevrant-home-services-$ENVIRONMENT -f ./database.yml"
            sh "kubectl rollout status deployments $REPOSITORY-db-deployment -n zevrant-home-services-$ENVIRONMENT"
            print "Deploying liquibase updates"
            def serviceName = REPOSITORY.tokenize("-")[1]
            def POSTGRESS_PASSWORD = credentials("/$ENVIRONMENT/rds/$serviceName/password")
            sh "POSTGRES_PASSWORD=$POSTGRESS_PASSWORD ./gradlew liquibase update"
        }
    }

    stage("Deploy") {
        sh "VERSION=$VERSION envsubst < deployment.yml | ENVIRONMENT=$ENVIRONMENT envsubst | kubectl apply -n zevrant-home-services-$ENVIRONMENT -f -"
        sh "kubectl rollout status deployments $REPOSITORY-deployment -n zevrant-home-services-$ENVIRONMENT"
    }
}