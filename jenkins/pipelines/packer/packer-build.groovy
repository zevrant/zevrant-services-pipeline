import com.zevrant.services.pojo.Version
@Library("CommonUtils")

import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.HashingService
import com.zevrant.services.services.VersionService
import org.apache.commons.lang.StringUtils

HashingService hashingService = new HashingService(this)
GitService gitService = new GitService(this)
VersionService versionService = new VersionService(this)

PackerCodeUnit codeUnit = PackerCodeUnitCollection.findCodeUnitByName(NAME as String)
String imageHash = ''
Version version = null
pipeline {
    agent {
        label 'container-builder'
    }
    stages {

        stage('Get Source Material') {
            steps {
                script {
                    gitService.checkout('git@github.com', 'zevrant', 'packer-build-specs',
                                  'master', 'jenkins-git')
                }
            }
        }

        stage('Validate Base Image') {
            steps {
                script {
                    retry(2, { //Retry is in case of a concurrent updates
                        if (StringUtils.isBlank(codeUnit.baseImageName)) {
                            //TODO Add GPG validation https://wiki.almalinux.org/cloud/Generic-cloud.html#verify-almalinux-9-images
                            String baseImageHashes = httpRequest(url: 'https://repo.almalinux.org/almalinux/9/cloud/x86_64/images/CHECKSUM').content
                            imageHash = baseImageHashes.split("\n")
                                    .find { hash -> hash.contains('latest.x86_64') }
                                    .split('\\h')[0]
                        } else {
                            String baseImageHash = readFile(file: "/opt/vm-images/${codeUnit.baseImageName}.sha512")
                            if (hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.baseImageName}.qcow2").replace("/opt/vm-images/", "") != baseImageHash) {
                                throw new RuntimeException("Failed to match file hash to specified base image, SOMETHING IS VERY WRONG HERE")
                            }
                            imageHash = baseImageHash
                        }
                    })
                }
            }

        }

        stage('Get New Version') {
            environment {
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGHOST = '192.168.0.101'
            }
            steps {
                script {
                    version = versionService.getVersion(codeUnit.name, true)
                    version = versionService.minorVersionUpdate(codeUnit.name, version, true)
                }
            }
        }

        stage('Build Image') {
            environment {
                TMPDIR="tmp"
            }
            steps {
                script {
                    dir(codeUnit.folderPath) {
                        dir("tmp") {
                            writeFile file:'dummy', text:''
                        }
                        writeYaml(file: 'vars.yaml', data: codeUnit.extraArguments)
                        sh 'packer init .'
                        String additionalArgs = ""

                        println(codeUnit.baseImageName)
                        if (StringUtils.isNotBlank(codeUnit.baseImageName)) {
                            additionalArgs = "-var 'base_image_path=/opt/vm-images/${codeUnit.baseImageName}.qcow2'"
                        }

                        sh "packer build -var base_image_hash=${imageHash.split('\\h')[0]} ${additionalArgs} ."
                        sh "mv build-output/packer-${codeUnit.name} build-output/${codeUnit.name}-${version.toThreeStageVersionString()}.qcow2"
                    }
                }
            }
        }

        stage('Upload Image & Hash') {
            steps {
                script {
                    dir(codeUnit.folderPath + "/build-output") {
                        String filehash = hashingService.getSha512SumFor("${codeUnit.name}-${version.toThreeStageVersionString()}.qcow2")
                        writeFile(file: "/opt/vm-images/${codeUnit.name}-${version.toThreeStageVersionString()}.sha512", text: filehash)
                        println("Original filehash ${filehash}")
                        sh "mv ${codeUnit.name}-${version.toThreeStageVersionString()}.qcow2 /opt/vm-images/${codeUnit.name}-${version.toThreeStageVersionString()}.qcow2"
                        String newFilehash = hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.name}-${version.toThreeStageVersionString()}.qcow2").replace("/opt/vm-images/", "")
                        println("New filehash ${newFilehash}")
                        if (newFilehash != filehash) {
                            throw new RuntimeException("Failed to match file hash to the built image, SOMETHING IS VERY WRONG HERE")
                        }
                    }
                }
            }
        }
    }
}

