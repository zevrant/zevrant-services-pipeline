@Library('CommonUtils') _

import com.zevrant.services.TaskLoader
import com.zevrant.services.pojo.Version

import com.zevrant.services.services.VersionTasks


BRANCH_NAME = BRANCH_NAME.tokenize("/")
BRANCH_NAME = BRANCH_NAME[BRANCH_NAME.size() - 1];
currentBuild.displayName = "$REPOSITORY merging to $BRANCH_NAME"
Version version = null
String variant = (BRANCH_NAME == "master") ? "release" : 'develop'
String avdName = "jenkins-android-test-$BUILD_ID"
VersionTasks versionTasks = TaskLoader.load(binding, VersionTasks) as VersionTasks
byte[] b = new byte[2000];
Version versionCode = null;
boolean runTests = Boolean.parseBoolean(RUN_TESTS as String)
pipeline {
    agent {
        label 'master'
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
                    git credentialsId: 'jenkins-git', branch: BRANCH_NAME,
                            url: "git@github.com:zevrant/${REPOSITORY}.git"
                }
            }
        }

        stage("Unit Test") {
            when { expression { runTests} }
            steps {
                script {
                    sh "bash gradlew clean testDevelopTest"
                }
            }
        }

        stage("Integration Test Setup") {
            when { expression { runTests } }
            steps {
                script {
                    String startEmulator = "/opt/android/android-sdk/emulator/emulator -avd $avdName -no-window -no-boot-anim -no-snapshot-save -no-snapshot-load"
                    sh "echo no | /opt/android/android-sdk/cmdline-tools/5.0/bin/avdmanager create avd -n $avdName --abi google_apis_playstore/x86_64 --package \'system-images;android-30;google_apis_playstore;x86_64\'"
                    sh "nohup $startEmulator > nohup-${avdName}.out &"
                    sh script: "aws secretsmanager get-secret-value --region us-east-1 --secret-id android-secrets-initializer > secret.txt"
                    String secret = readJSON(file: 'secret.txt')["SecretString"]
                    sh 'rm secret.txt'
                    secret = secret.replaceAll("\\n", "")
                    writeFile(file: "secret.txt", text: secret)
                    writeFile(file: 'bashScript.sh', text: """
#!/bin/bash
cat secret.txt | base64 --decode > app/src/androidTest/java/com/zevrant/services/zevrantandroidapp/secrets/SecretsInitializer.java
""")
                    sh "bash bashScript.sh && rm secret.txt"
                    echo "waiting for emulator to come online"
                    String offline = "offline"
                    while (offline.contains("offline")) {
                        sh 'sleep 5'
                        offline = sh returnStdout: true, script: '/opt/android/android-sdk/platform-tools/adb devices'
                        echo offline
                    }
                    echo 'restarting adb to keep device from showing as unauthorized'
                    int status = sh 'set -e /opt/android/android-sdk/platform-tools/adb kill-server && /opt/android/android-sdk/platform-tools/adb start-server'
                    int i = 0;
                    while (status != 0 && i < 10) {
                        sleep 3
                        status = sh returnStatus: true, script: 'set -e /opt/android/android-sdk/platform-tools/adb kill-server && /opt/android/android-sdk/platform-tools/adb start-server'
                        println "status is " + status
                        i++
                    }
                    if (i == 10) {
                        echo "Failed to restart adb"
                        throw RuntimeException("Failed to restart ADB")
                    }
                    offline = "offline"
                    while (offline.contains("offline")) {
                        sh 'sleep 5'
                        offline = sh returnStdout: true, script: '/opt/android/android-sdk/platform-tools/adb devices'
                        echo offline
                    }
                }
            }
            post {
                failure {
                    script {
                        if(runTests) {
                            String pid = sh returnStdout: true, script: ' set -e pgrep qemu-system-x86'
                            if (pid != "" && pid != null) {
                                echo "killing emulator with pid $pid"
                                sh "kill -9 $pid"
                                echo "deleting avd with name $avdName"
                                sh "/opt/android/android-sdk/cmdline-tools/5.0/bin/avdmanager delete avd -n $avdName"
                            }
                            archiveArtifacts artifacts: "nohup-${avdName}.out", followSymlinks: false
                        }
                    }
                }
            }
        }

        stage("Integration Test") {
            when { expression { runTests } }
            steps {
                script {
                    sh 'bash gradlew clean connectedDevelopTest'
                }
            }
            post {
                always {
                    script {
                        if(RUN_TESTS) {
                            try {
                                sh 'ADB_COMMAND="/opt/android/android-sdk/platform-tools/adb" bash gradlew pullReport'
                                if (fileExists("cucumber-reports/cucumber.xml")) {
                                    cucumber 'cucumber-reports/cucumber.json'
                                    sh "zip -r html-report.zip cucumber-reports/html-report"
                                    archiveArtifacts 'html-report.zip'
                                }
                            } finally {
                                String pid = sh returnStdout: true, script: 'pgrep qemu-system-x86'
                                if (pid != "" && pid != null) {
                                    echo "killing emulator with pid $pid"
                                    sh "kill -9 $pid"
                                    echo "deleting avd with name $avdName"
                                    sh "/opt/android/android-sdk/cmdline-tools/5.0/bin/avdmanager delete avd -n $avdName"
                                }
                                archiveArtifacts artifacts: 'nohup.out', followSymlinks: false
                            }
                        }
                    }
                }
            }
        }


        stage("Release Version Update") {
            when { expression { variant == 'releaseNEVER' } }
            steps {
                script {
                    versionTasks.majorVersionUpdate(REPOSITORY as String, version)
                }
            }
        }

        stage("Development Version Update") {
            when { expression { variant == 'NEVER' } }
            steps {
                script {
                    versionTasks.minorVersionUpdate(REPOSITORY as String, version)
                    versionTasks.versionIncrement(REPOSITORY as String, versionCode)

                }
            }
        }

        stage("Build Artifact") {
            steps {
                script {
                    def json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/keystore"))
                    String keystore = json['SecretString']; writeFile file: './zevrant-services.txt', text: keystore
                    sh "base64 -d ./zevrant-services.txt > ./zevrant-services.p12"
                    json = readJSON text: (sh(returnStdout: true, script: "aws secretsmanager get-secret-value --secret-id /android/signing/password"))
                    String password = json['SecretString']
                    sh " SIGNING_KEYSTORE=\'${env.WORKSPACE}/zevrant-services.p12\' " + 'KEYSTORE_PASSWORD=\'' + password + "\' bash gradlew clean assemble${variant.capitalize()} --no-daemon -PprojVersion='${version.toThreeStageVersionString()}' -PversionCode='${zversionCode}'"
                    //for some reason gradle isn't signing like it's suppost to so we do it manually
                    sh "keytool -v -importkeystore -srckeystore zevrant-services.p12 -srcstoretype PKCS12 -destkeystore zevrant-services.jks -deststoretype JKS -srcstorepass \'$password\' -deststorepass \'$password\' -noprompt"

                    sh "/opt/android/android-sdk/build-tools/31.0.0/zipalign -p -f -v 4 app/build/outputs/apk/$variant/app-${variant}.apk zevrant-services-unsigned.apk"
                    sh "/opt/android/android-sdk/build-tools/31.0.0/apksigner sign --min-sdk-version 29 --ks zevrant-services.jks --in ./zevrant-services-unsigned.apk --out ./zevrant-services.apk --ks-pass \'pass:$password\'"
                    sh "/opt/android/android-sdk/build-tools/31.0.0/apksigner verify -v zevrant-services.apk"
                }
            }
        }

        stage("Release to Google Play") {
//            when { expression { variant == 'release'}}
            steps {
                script {
                    androidApkMode(
                            googleCredentialsId: 'jenkins-gcp',
                            trackName: 'production',
                            rolloutPercentage: '100',
                            fromVersionCode: true,
                            applicationId: "com.zevrant.services.${((String) REPOSITORY).toLowerCase().replaceAll("-", "")}",
                            versionCodes: versionCode.toVersionCodeString(),
                            filesPattern: 'zevrant-services.apk'
                    )
                }
            }
        }
    }
}