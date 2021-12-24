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
                        sh "kubectl get pods --no-headers=true | awk '/Error/{print \$1}' | xargs kubectl delete pods"
                        sh "kubectl get pods --no-headers=true | awk '/Complete/{print \$1}' | xargs kubectl delete pods"
                    }
                }
            }
        }
    }
}
