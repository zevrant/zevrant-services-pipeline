@Library("CommonUtils")

import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection
import com.zevrant.services.services.HashingService

HashingService hashingService = new HashingService(this)

PackerCodeUnit codeUnit = PackerCodeUnitCollection.findCodeUnitByName(NAME as String)
String imageHash = ''
String outputFileName = "build-output/${codeUnit.name}.qcow2"
pipeline {

    agent {
        label 'container-build'
    }
    stages {
        stage ('Validate Base Image') {
            steps {
                script {
                    retry(2, { //Retry is in case of a concurrent updates
                        String baseImageHash = readFile(file: "/opt/vm-images/${codeUnit.baseImageName}.sha512")
                        if (hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.baseImageName}.qcow2") != baseImageHash) {
                            throw new RuntimeException("Failed to match file hash to specified base image, SOMETHING IS VERY WRONG HERE")
                        }
                        imageHash = baseImageHash
                    })
                }
            }

        }

        stage ('Build Image') {
            steps {
                script {
                    dir (codeUnit.folderPath) {
                        writeYaml(codeUnit.extraArguments)
                        sh 'packer build .'
                        sh "mv build-output/${codeUnit.name} $outputFileName"
                    }
                }
            }
        }

        stage ('Upload Image & Hash') {
            steps {
                script {
                    String filehash = hashingService.getSha512SumFor(outputFileName)
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

