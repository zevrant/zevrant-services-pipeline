@Library("CommonUtils")

import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection
import com.zevrant.services.services.GitService
import com.zevrant.services.services.HashingService
import org.apache.commons.lang.StringUtils

HashingService hashingService = new HashingService(this)
GitService gitService = new GitService(this)

PackerCodeUnit codeUnit = PackerCodeUnitCollection.findCodeUnitByName(NAME as String)
String imageHash = ''
String outputFileName = "build-output/${codeUnit.name}.qcow2"
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
                            if (hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.baseImageName}.qcow2") != baseImageHash) {
                                throw new RuntimeException("Failed to match file hash to specified base image, SOMETHING IS VERY WRONG HERE")
                            }
                            imageHash = baseImageHash
                        }
                    })
                }
            }

        }

        stage('Build Image') {
            steps {
                script {
                    dir(codeUnit.folderPath) {
                        sh 'rm -f vars.yaml'
                        writeYaml(file: 'vars.yaml', data: codeUnit.extraArguments)
                        sh 'packer init .'
                        sh "packer build -var base_image_hash=${imageHash} ."
                        sh "mv build-output/packer-${codeUnit.name} $outputFileName"
                    }
                }
            }
        }

        stage('Upload Image & Hash') {
            steps {
                script {
                    dir(codeUnit.folderPath) {
                        String filehash = hashingService.getSha512SumFor("build-output/${codeUnit.name}.qcow2")
                        writeFile(file: "/opt/vm-images/${codeUnit.name}.sha512", text: filehash)
                        sh "mv ${outputFileName} /opt/vm-images/${codeUnit.name}.qcow2"
                        if (hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.baseImageName}.qcow2") != filehash) {
                            throw new RuntimeException("Failed to match file hash to the built image, SOMETHING IS VERY WRONG HERE")
                        }
                    }
                }
            }
        }
    }
}

