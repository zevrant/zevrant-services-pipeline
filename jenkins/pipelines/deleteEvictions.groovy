pipeline {
    agent {
        kubernetes {
            inheritFrom 'kubernetes'
        }
    }

    stages {
        stage("Delete Evictions") {
            environment {
                KUBECONFIG = credentials('jenkins-kubernetes')
            }
            steps {
                container('kubectl') {
                    script {
                        sh "kubectl delete pods --field-selector=status.phase=Failed --all-namespaces"
                    }
                }
            }
        }
    }
}
