
List<String> hosts = [
        'zevrant-01.zevrant-services.com',
        'zevrant-02.zevrant-services.com',
        'zevrant-03.zevrant-services.com',
        'zevrant-04.zevrant-services.com',
        'zevrant-05.zevrant-services.com'
]

pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }
    stages {
        stage ('Restart DNS Servers') {
            steps {
                script {
                    sshagent(credentials: ['jenkins-git']) {
                        hosts.each { host ->
                            sh "ssh -o StrictHostKeyChecking=no jenkins@${host} sudo systemctl restart bind9"
                        }
                    }
                }
            }
        }
    }
}