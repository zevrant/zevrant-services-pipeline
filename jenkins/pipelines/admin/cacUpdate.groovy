@Library("CommonUtils") _


import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.services.NotificationService
import io.jenkins.plugins.casc.ConfigurationAsCode


List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
pipeline {
    agent {
        label('built-in')
    }

    stages {
        stage("Validate Yaml") {
            steps {
                script {
                    ((String) sh(returnStdout: true, script: 'ls *.yml'))
                            .split("[\\h\\n]")
                            .each { fileName ->
                                //will fail if invalid yaml
                                def jenkinsCacYaml = readYaml(file: fileName)
                            }
                }
            }
        }
        stage('Deploy') {
            when { expression { BRANCH_NAME == 'main' } }
            steps {
                script {
                    sh 'ls -l /var/lib/jenkins/casc_configs'
                    sh 'rm -f /var/lib/jenkins/casc_configs/*'
                    ((String) sh(returnStdout: true, script: 'ls *.yml'))
                            .split("[\\h\\n]")
                            .each { fileName ->
                                sh "cp '${fileName}' /var/lib/jenkins/casc_configs/${fileName}"
                            }
                }
            }
        }
        stage('Reload Config') {
            when { expression { BRANCH_NAME == 'main' } }
            steps {
                script {
                    retry ( 3, {
                        ConfigurationAsCode.get().configure()
                    })
                }
            }
        }
    }
    post {
        always {
            script {
                String appName = REPOSITORY.split("-").each { it.capitalize() }.join(" ")
                new NotificationService(this).notifcationService.sendDiscordNotification("Jenkins Build for ${appName} on branch ${branchName} ${currentBuild.currentResult}", env.BUILD_URL, currentBuild.currentResult, "Jenkins CaC Update", NotificationChannel.DISCORD_CICD)
            }
        }
    }
}
