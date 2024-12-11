import com.zevrant.services.pojo.ProviderShasumsLinks
import com.zevrant.services.pojo.codeunit.GoCodeUnit
import com.zevrant.services.pojo.codeunit.GoCodeUnitCollection
import com.zevrant.services.services.TerraformCloudProviderBinary
import com.zevrant.services.services.TerraformCloudService

TerraformCloudService terraformCloudService = new TerraformCloudService(this)


GoCodeUnit codeUnit = GoCodeUnitCollection.findCodeUnitByRepositoryName(REPOSITORY)
List<String> providerOs = [
        "freebsd",
        "linux",
        "windows"
]

List<String> providerArch = [
        "amd64",
        "arm64",
        "arm",
        "386"
]
String taggedVersion = ''
pipeline {
    agent {
        label 'container-builder'
    }

    stages {
        stage('Get Artifacts') {
            steps {
                script {
                    copyArtifacts filter: 'artifactVersion.txt', fingerprintArtifacts: true, projectName: "./${codeUnit.name.split('-').collect({ item -> item.capitalize() }).join(' ')}-multibranch/master"
                    taggedVersion = readFile(file: 'artifactVersion.txt')
                    dir('dist') {
                        withCredentials([string(credentialsId: 'jenkins-git-access-token-as-text', variable: 'token')]) {
                            providerOs.each { os ->
                                providerArch.each { arch ->
                                    httpRequest(
                                            url: "https://github.com/zevrant/${codeUnit.name}/releases/download/${taggedVersion}/${codeUnit.name}_${taggedVersion.replace('v', '')}_${os}_${arch}.zip",
                                            outputFile: "${codeUnit.name}_${taggedVersion}_${os}_${arch}.zip",
                                            customHeaders: [
                                                    [
                                                            'name'     : "Authorization",
                                                            'value'    : "bearer " + token.replace('"', ''),
                                                            'maskValue': true
                                                    ]
                                            ]
                                    )
                                }
                            }
                            httpRequest(
                                    url: "https://github.com/zevrant/${codeUnit.name}/releases/download/${taggedVersion}/${codeUnit.name}_${taggedVersion.replace('v', '')}_SHA256SUMS",
                                    outputFile: "${codeUnit.name}_${taggedVersion.replace('v', '')}_SHA256SUMS",
                                    customHeaders: [
                                            [
                                                    'name'     : "Authorization",
                                                    'value'    : "bearer " + token.replace('"', ''),
                                                    'maskValue': true
                                            ]
                                    ]
                            )
                            httpRequest(
                                    url: "https://github.com/zevrant/${codeUnit.name}/releases/download/${taggedVersion}/${codeUnit.name}_${taggedVersion.replace('v', '')}_SHA256SUMS.sig",
                                    outputFile: "${codeUnit.name}_${taggedVersion.replace('v', '')}_SHA256SUMS.sig",
                                    customHeaders: [
                                            [
                                                    'name'     : "Authorization",
                                                    'value'    : "bearer " + token.replace('"', ''),
                                                    'maskValue': true
                                            ]
                                    ]
                            )
                        }
                    }
                }
            }
        }

        stage("Push to Terraform Cloud") {
            environment {
                terraformCloudToken = credentials('terraform-cloud-token')
            }
            steps {
                script {
                    sh 'echo "$terraformCloudToken" > tftoken'
                    String terraformCloudToken = readFile(file: 'tftoken')
                    String gpgKeyId = terraformCloudService.getLatestGPGKeyId(codeUnit.providerOrgName, terraformCloudToken)
                    String providerName = codeUnit.name.replace('terraform-provider-', '')
                    ProviderShasumsLinks shasumsLinks = terraformCloudService
                            .createProviderVersion(gpgKeyId, terraformCloudToken as String, codeUnit.providerOrgName,
                                    providerName, taggedVersion)

                    dir('dist') {
                        terraformCloudService.uploadFile("${codeUnit.getName()}_${taggedVersion}_SHA256SUMS", terraformCloudToken as String, shasumsLinks.getShasumsUpload())
                        terraformCloudService.uploadFile("${codeUnit.getName()}_${taggedVersion}_SHA256SUMS.sig", terraformCloudToken as String, shasumsLinks.getShasumsSigUpload())

                        String shasums = readFile(file: "${codeUnit.getName()}_${taggedVersion}_SHA256SUMS" as String)

                        List<TerraformCloudProviderBinary> binariesUploadList = []

                        shasums.split("\n").findAll({ line -> line.trim() != '' && !line.contains('.json') }).each { line ->
                            TerraformCloudProviderBinary binary = new TerraformCloudProviderBinary()
                            String[] binaryInfoPart = line.split("\\h").findAll { part -> part.trim() != '' }
                            binary.setShasum(binaryInfoPart[0])
                            binary.setFileName(binaryInfoPart[1])
                            println(binaryInfoPart[1])
                            String[] fileNameParts = binaryInfoPart[1].replace('.zip', '').split("_")

                            binary.setArch(fileNameParts[3])
                            binary.setOs(fileNameParts[2])
                            binariesUploadList.add(binary)
                        }

                        binariesUploadList.each { binary ->
                            String uploadLink = terraformCloudService.createProviderPlatform(terraformCloudToken as String, codeUnit.providerOrgName, codeUnit.name, taggedVersion, binary)
                            terraformCloudService.uploadFile(binary.getFileName(), terraformCloudToken as String, uploadLink)
                        }

                    }
                }
            }
        }
    }
}