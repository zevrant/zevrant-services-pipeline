@Library("CommonUtils") _


import com.lesfurets.jenkins.unit.global.lib.Library
import com.zevrant.services.ServiceLoader
import com.zevrant.services.pojo.Version
import com.zevrant.services.services.VersionService
import io.jenkins.plugins.casc.ConfigurationAsCode

List<String> angularProjects = ["zevrant-home-ui"];

String branchName = (BRANCH_NAME.startsWith('PR-')) ? CHANGE_BRANCH : BRANCH_NAME
VersionService versionTasks = ServiceLoader.load(binding, VersionService) as VersionService
Version version = null
String REPOSITORY = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
pipeline {
    agent {
        label('master-node')
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
            when { expression { BRANCH_NAME == 'master' } }
            steps {
                script {
                    sh 'ls -l /var/jenkins_home/casc_configs'
                    sh 'rm -f /var/jenkins_home/casc_configs/*'
                    ((String) sh(returnStdout: true, script: 'ls *.yml'))
                            .split("[\\h\\n]")
                            .each { fileName ->
                                sh "cp '${fileName}' /var/jenkins_home/casc_configs/${fileName}"
                            }
                }
            }
        }
        stage('Reload Config') {
            when { expression { BRANCH_NAME == 'master' } }
            steps {
                script {
                    ConfigurationAsCode.get().configure()
                }
            }
        }
    }
    post {
        always {
            script {
                String appName = REPOSITORY.split("-").each { it.capitalize() }.join(" ")
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Build for ${appName} on branch ${branchName} ${currentBuild.currentResult}", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Jenkins CaC Update", webhookURL: webhookUrl
                }
            }
        }
    }
}
