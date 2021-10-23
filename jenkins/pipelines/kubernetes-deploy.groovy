pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }
    stages {
        stage("SCM Checkout") {
            steps {
                container('kubectl') {
                    script {
                        currentBuild.displayName = "Deploying $REPOSITORY version $VERSION"
                        git credentialsId: 'jenkins-git', branch: (ENVIRONMENT = 'prod') ? 'master' : ENVIRONMENT,
                                url: "git@github.com:zevrant/${REPOSITORY}.git"
                    }
                }
            }
        }
        stage("Deploy Database") {
            when { expression { fileExists('database.yml') } }
            steps {
                container('kubectl') {
                    script {
                        print ENVIRONMENT
                        sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./database.yml"
                        sh "kubectl apply -n zevrant-home-services-$ENVIRONMENT -f ./database.yml"
                        sh "kubectl rollout status deployments $REPOSITORY-db-deployment -n zevrant-home-services-$ENVIRONMENT"
                    }
                }
            }
        }

        stage("Deploy") {
            when { expression { fileExists('database.yml') } }
            steps {
                container('kubectl') {
                    script {
                        sh "sed -i 's/\$ENVIRONMENT/$ENVIRONMENT/g' ./deployment.yml"
                        sh "sed -i 's/\$VERSION/$VERSION/g' ./deployment.yml"
                        sh "kubectl apply -n zevrant-home-services-$ENVIRONMENT -f ./deployment.yml"
                        sh "kubectl rollout status deployments $REPOSITORY-deployment -n zevrant-home-services-$ENVIRONMENT"
                    }
                }
            }
        }
    }
}