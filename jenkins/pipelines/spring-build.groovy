@Library("CommonUtils") _

import groovy.json.JsonSlurper
import com.zevrant.services.pojo.Version

def angularProjects = ["zevrant-home-ui"];
def environments = ["develop", "prod"];
BASE_BRANCH = BASE_BRANCH.tokenize("/")
BASE_BRANCH = BASE_BRANCH[BASE_BRANCH.size() - 1];
Version version = null;
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }
    environment {
        AWS_ACCESS_KEY_ID= credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY= credentials('aws-secret-access-key')
        AWS_DEFAULT_REGION="us-east-1"
    }
    stages {
        stage("Get Version") {
            steps {
                script {
                    version = versionTasks.getVersion(REPOSITORY as String)
                    versionCode = versionTasks.getVersionCode("${REPOSITORY.toLowerCase()}")
                    echo RUN_TESTS
                }
            }
        }

        stage("SCM Checkout") {
            steps {
                script {
                    git credentialsId: 'jenkins-git', branch: BASE_BRANCH,
                            url: "git@github.com:zevrant/${REPOSITORY}.git"
                }
            }
        }

        stage("Test") {
            steps {
                script {
                    if (angularProjects.indexOf(REPOSITORY) > 2) {
                        sh "npm run test"
                    } else {
                        "bash gradlew clean build --no-daemon"
                    }
                }
            }

        }
        stage("Develop Version Update") {
            when { expression { BASE_BRANCH == "develop" } }
            steps {
                script {
                    versionTasks.minorVersionUpdate(REPOSITORY, version)
                }
            }
        }

        stage("Release Version Update") {
            when { expression { BASE_BRANCH == "master" } }
            steps {
                script {
                    versionTasks.majorVersionUpdate(REPOSITORY, version)
                }
            }
        }

        stage("Build Artifact") {
            steps {
                script {
                    sh "bash gradlew clean assemble --no-daemon"
                    sh "docker build -t zevrant/$REPOSITORY:$version ."
                    sh "docker push zevrant/$REPOSITORY:$version"
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    String env = (BASE_BRANCH == "master")? "prod" : "develop"
                    if(env == "prod") {
                        sh "docker tag zevrant/${REPOSITORY}:${version} zevrant/${REPOSITORY}:${newVersion}"
                        sh "docker push zevrant/${REPOSITORY}:${newVersion}"
                    }
                    build job: 'Deploy', parameters: [
                            [$class: 'StringParameterValue', name: 'REPOSITORY', value: REPOSITORY],
                            [$class: 'StringParameterValue', name: 'VERSION', value: version],
                            [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "develop"]
                    ]
                }
            }
        }
    }
}