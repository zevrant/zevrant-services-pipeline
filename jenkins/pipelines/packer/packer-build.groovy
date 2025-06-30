import com.zevrant.services.pojo.GitHubArtifactMapping
import com.zevrant.services.pojo.Version
@Library("CommonUtils")

import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection
import com.zevrant.services.services.GitHubService
import com.zevrant.services.services.GitService
import com.zevrant.services.services.HashingService
import com.zevrant.services.services.VersionService
import org.apache.commons.lang.StringUtils

HashingService hashingService = new HashingService(this)
GitService gitService = new GitService(this)
VersionService versionService = new VersionService(this)
GitHubService gitHubService = new GitHubService(this)


PackerCodeUnit codeUnit = PackerCodeUnitCollection.findCodeUnitByName(NAME as String)
String imageHash = ''
Version version = null
String baseImageVersion = ''
pipeline {
    agent {
        label 'container-builder'
    }
    stages {

        stage('Get Source Material') {
            steps {
                script {
                    gitService.checkout(codeUnit.specRepo.sshHostName, codeUnit.specRepo.org, codeUnit.specRepo.repoName,
                            'master', 'jenkins-git')
                }
            }
        }

        stage('Validate Base Image') {
            steps {
                script {
                    retry(2, { //Retry is in case of a concurrent updates
                        if (codeUnit.name == 'ubuntu-server-base-image') {
                            String baseImageHashes = httpRequest(url: 'https://cloud-images.ubuntu.com/noble/current/SHA256SUMS').content
                            imageHash = baseImageHashes.split("\n")
                                    .find { hash -> hash.contains('noble-server-cloudimg-amd64.img') }
                                    .split('\\h')[0]
                        } else if (StringUtils.isBlank(codeUnit.baseImageName)) {
                            //TODO Add GPG validation https://wiki.almalinux.org/cloud/Generic-cloud.html#verify-almalinux-9-images
                            String baseImageHashes = httpRequest(url: 'https://repo.almalinux.org/almalinux/9/cloud/x86_64/images/CHECKSUM').content
                            imageHash = baseImageHashes.split("\n")
                                    .find { hash -> hash.contains('latest.x86_64') }
                                    .split('\\h')[0]
                        } else {
                            copyArtifacts(filter: 'artifactVersion.txt', projectName: "build-${codeUnit.baseImageName}", selector: lastSuccessful())
                            baseImageVersion = readFile('artifactVersion.txt')
                            String baseImageHash = readFile(file: "/opt/vm-images/${codeUnit.baseImageName}-${baseImageVersion}.sha512")
                            if (hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.baseImageName}-${baseImageVersion}.qcow2").replace("/opt/vm-images/", "") != baseImageHash) {
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
                PGHOST = '10.1.0.18'
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGSSLMODE = 'disable'
                PGDATABASE = 'jenkins'
            }
            steps {
                script {
                    version = versionService.getVersion(codeUnit.name, true)
                    println("Version received is ${version.toSemanticVersionString()}")
                    version = versionService.patchVersionUpdate(codeUnit.name, version, true)
                    currentBuild.displayName = "Building Version ${version.toSemanticVersionString()}" as String
                }
            }
        }

        stage('Build Image') {
            environment {
                TMPDIR = "tmp"
            }
            steps {
                script {
                    sh 'ls -l'
                    dir(codeUnit.folderPath) {
                        dir("tmp") {
                            writeFile file: 'dummy', text: ''
                        }
                        if (codeUnit.extraArguments != null && !codeUnit.extraArguments.isEmpty()) {
                            codeUnit.extraArguments.keySet().each { key ->
                                Object argument = codeUnit.extraArguments.get(key)
                                if (argument instanceof GitHubArtifactMapping) {
                                    String response = gitHubService.getLatestRelease(argument.getGitHubRepoOwner(), argument.getGitHubRepo())
                                    codeUnit.extraArguments[key] = gitHubService.getDownloadUrlFromAssetsResponse(response)
                                }
                            }

                            writeYaml(file: 'vars.yaml', data: codeUnit.extraArguments)
                        }
                        sh 'packer init .'
                        String additionalArgs = ""

                        println(codeUnit.baseImageName)
                        if (StringUtils.isNotBlank(codeUnit.baseImageName)) {
                            additionalArgs = "-var 'base_image_path=/opt/vm-images/${codeUnit.baseImageName}-${baseImageVersion}.qcow2'"
                        }

                        sh "packer build -var base_image_hash=${imageHash.split('\\h')[0]} ${additionalArgs} ."
                        sh "mv build-output/packer-${codeUnit.name} build-output/${codeUnit.name}-${version.toSemanticVersionString()}.qcow2"
                    }
                }
            }
        }

        stage('Upload Image & Hash') {
            steps {
                script {
                    dir(codeUnit.folderPath + "/build-output") {
                        String filehash = hashingService.getSha512SumFor("${codeUnit.name}-${version.toSemanticVersionString()}.qcow2")
                        String shaFile = "${codeUnit.name}-${version.toSemanticVersionString()}.sha512"
                        writeFile(file: shaFile, text: filehash)
                        sh "mv ${shaFile} /opt/vm-images/${shaFile}"
                        println("Original filehash ${filehash}")
                        sh "mv ${codeUnit.name}-${version.toSemanticVersionString()}.qcow2 /opt/vm-images/${codeUnit.name}-${version.toSemanticVersionString()}.qcow2"
                        String newFilehash = hashingService.getSha512SumFor("/opt/vm-images/${codeUnit.name}-${version.toSemanticVersionString()}.qcow2").replace("/opt/vm-images/", "")
                        println("New filehash ${newFilehash}")
                        if (newFilehash != filehash) {
                            throw new RuntimeException("Failed to match file hash to the built image, SOMETHING IS VERY WRONG HERE")
                        }
                    }
                }
            }
        }

        stage("Cleanup Old Images") {
            steps {
                script {
                    String output = sh(returnStdout: true, script: "ls -lt /opt/vm-images/${codeUnit.name}*")
                    List<String> imageNames = []
                    String[] lines = output.split('\n')
                    if (lines.length < 8) {
                        for (int i = 0; i < lines.length - 8; i++) {
                            String line = lines[i]
                            if (line.startsWith("total")) {
                                continue
                            }
                            String imagePath = line.split(' ')
                                    .find({ part -> part.contains("qcow2") || part.contains("sha512") })
                            imageNames.add(
                                    imagePath.split('/').find({ part -> part.contains("qcow2") || part.contains("sha512") })
                            )
                        }
                        imageNames.each { toBeRemoved ->
                            if (StringUtils.isNotBlank(toBeRemoved.trim())) {
                                print("Removing /opt/vm-images/${toBeRemoved}")
                                sh "rm /opt/vm-images/${toBeRemoved}"
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                writeFile(file: "artifactVersion.txt", text: version.toSemanticVersionString())
                archiveArtifacts(artifacts: 'artifactVersion.txt', allowEmptyArchive: false)
            }
        }
    }
}

