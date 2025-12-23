package packer


import com.zevrant.services.pojo.ProxmoxVolume
import com.zevrant.services.pojo.Version
@Library("CommonUtils")

import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.pojo.codeunit.PackerCodeUnitCollection
import com.zevrant.services.services.*
import org.apache.commons.lang.StringUtils

HashingService hashingService = new HashingService(this)
GitService gitService = new GitService(this)
VersionService versionService = new VersionService(this)
GitHubService gitHubService = new GitHubService(this)
ProxmoxQueryService proxmoxQueryService = new ProxmoxQueryService(this)
SecretsService secretsService = new SecretsService(this)
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
            environment {
                PGHOST = '10.1.0.18'
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGSSLMODE = 'disable'
                PGDATABASE = 'jenkins'
            }
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

                            imageHash = versionService.getImageHashForVersion(new Version(baseImageVersion), codeUnit.baseImageName)
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
//                    version = versionService.patchVersionUpdate(codeUnit.name, version, true)
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
            environment {
                VAULT_TOKEN = credentials('local-vault')
                PGHOST = '10.1.0.18'
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGSSLMODE = 'disable'
                PGDATABASE = 'jenkins'
            }
            steps {
                script {
                    println("upload")
                    String vaultToken = secretsService.getLocalApiToken(VAULT_TOKEN_USR, VAULT_TOKEN_PSW)
                    Map<String, String> response = secretsService.getLocalSecret(vaultToken, '/proxmox/jenkins-token')
                    proxmoxQueryService.setProxmoxCredentials(response.username, response.password)
                    dir(codeUnit.folderPath + "/build-output") {
                        String filehash = hashingService.getSha512SumFor("${codeUnit.name}-${version.toSemanticVersionString()}.qcow2")
                        String shaFile = "${codeUnit.name}-${version.toSemanticVersionString()}.sha512"
                        writeFile(file: shaFile, text: filehash)
                        proxmoxQueryService.uploadImage("vm-images", "proxmox-01", "${codeUnit.name}-${version.toSemanticVersionString()}.qcow2", filehash)
                        versionService.addImageHashMapping(version, codeUnit.name, filehash)

                    }
                }
            }
        }

        stage("Cleanup Old Images") {
            environment {
                VAULT_TOKEN = credentials('local-vault')
                PGHOST = '10.1.0.18'
                PGUSER = 'jenkins'
                PGPASSWORD = credentials('jenkins-app-version-password')
                PGSSLMODE = 'disable'
                PGDATABASE = 'jenkins'
            }
            steps {
                script {
                    //TODO: make the next 3 lines a method call
                    String vaultToken = secretsService.getLocalApiToken(VAULT_TOKEN_USR, VAULT_TOKEN_PSW)
                    Map<String, String> response = secretsService.getLocalSecret(vaultToken, '/proxmox/jenkins-token')
                    proxmoxQueryService.setProxmoxCredentials(response.username, response.password)

                    println("list stored volumes")
                    List<ProxmoxVolume> volumes = proxmoxQueryService.listStoredVolumes("vm-images", "proxmox-01")
                            .findAll({ volume -> volume.volumeName.replaceAll("-\\d+\\.\\d+\\.\\d+\\.qcow2", "") == codeUnit.name })

                    println(volumes)
                    volumes = proxmoxQueryService.sortVolumesByVersion(volumes)

                    for (int i = 0; i < volumes.size(); i++) {
                        println(volumes.get(i).volumeName)
                    }
                    println("Volumes found " + volumes.size())
                    if (volumes.size() > 8) {
                        volumes.subList(volumes.size() - 8, volumes.size()).each { volume ->
                            proxmoxQueryService.deleteImage("vm-images", "proxmox-01", volume.volid)
                            String fileName = volume.volid.split("/")[1]
                            String[] fileNameParts = fileName.split("-")
                            String fileHash = versionService.getImageHashForVersion(new Version(fileNameParts[fileNameParts.length - 1].replace(".qcow2", "")), codeUnit.name)
                            versionService.deleteImageHashMapping(fileHash)
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

