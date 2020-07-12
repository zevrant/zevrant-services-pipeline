import java.util.regex.Pattern;

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
            def POSTGRESS_PASSWORD = sh returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /$ENVIRONMENT/rds/$serviceName/password | jq .SecretString"
            POSTGRESS_PASSWORD = POSTGRESS_PASSWORD.replaceAll(Pattern.compile("\""), "");
            print POSTGRESS_PASSWORD;
            sh "POSTGRES_PASSWORD=$POSTGRESS_PASSWORD ./gradlew liquibase update"
        }
    }

    stage("Deploy") {
        sh "VERSION=$VERSION envsubst < deployment.yml | ENVIRONMENT=$ENVIRONMENT envsubst | kubectl apply -n zevrant-home-services-$ENVIRONMENT -f -"
        sh "kubectl rollout status deployments $REPOSITORY-deployment -n zevrant-home-services-$ENVIRONMENT"
    }
}