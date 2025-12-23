package com.zevrant.services.services

import com.cloudbees.groovy.cps.NonCPS
import com.zevrant.services.pojo.ProxmoxVolume
import com.zevrant.services.pojo.Version

import java.nio.charset.StandardCharsets

public class ProxmoxQueryService extends Service {

    private static final String proxmoxUrl = "https://proxmox-01.zevrant-services.com:8006/api2/json";
    private String username;
    private String password;

    public ProxmoxQueryService(def pipelineContext) {
        super(pipelineContext);
    }

    public void setProxmoxCredentials(String username, String token) {
        this.username = username;
        this.password = token;
    }

    public List<ProxmoxVolume> listStoredVolumes(String storageName, String proxmoxNode) {
        def response = this.pipelineContext.httpRequest(
                httpMode: 'GET',
                url: "${proxmoxUrl}/nodes/${proxmoxNode}/storage/${storageName}/content",
                consoleLogResponseBody: true,
                customHeaders: [
                        [
                                'name'     : "Authorization",
                                'value'    : "PVEAPIToken=" + username + "=" + password,
                                'maskValue': true
                        ]
                ]
        )
        def responseContent = pipelineContext.readJSON(text: response.content)
        pipelineContext.println("Content: ${pipelineContext.writeJSON(returnText: true, json: responseContent.data)}")
        List<ProxmoxVolume> volumes = new ArrayList<>()
        for (volume in responseContent.data) {
            ProxmoxVolume proxmoxVolume = new ProxmoxVolume()
            proxmoxVolume.format = volume.format
            proxmoxVolume.size = volume.size
            proxmoxVolume.content = volume.content
            proxmoxVolume.ctime = volume.ctime
            proxmoxVolume.volid = volume.volid
            proxmoxVolume.volumeName = volume.volid.split("/")[1]
            volumes.add(proxmoxVolume)
        }

        volumes.sort { it.volumeName }

        return volumes
    }

    public ProxmoxVolume uploadImage(String storageName, String proxmoxNode, String imagePath, String imageChecksum) {

        def parameters = [
                "content": "import",
                "checksum": imageChecksum,
                "checksum-algorithm": "sha512",
        ]
        String params = ""
        for (key in parameters.keySet()) {
            if (params == "") {
                params = "?${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(parameters[key], StandardCharsets.UTF_8)}"
            } else {
                params += "&${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(parameters[key], StandardCharsets.UTF_8)}"
            }

        }


//        String taskId = pipelineContext.httpRequest(
//                url: "${proxmoxUrl}/nodes/${proxmoxNode}/storage/${storageName}/upload${params}",
//                httpMode: "POST",
//                wrapAsMultipart: true,
//                uploadFile: imagePath,
//                multipartName: getFilenameFromPath(imagePath),
//                requestBody: params,
//                customHeaders: [
//                        [
//                                'name'     : "Authorization",
//                                'value'    : "PVEAPIToken=" + username + "=" + password,
//                                'maskValue': true
//                        ],
//                        [
//                                'name' : 'CONTENT-TYPE',
//                                'value': 'multipart/form-data'
//                        ]
//                ],
//                validResponseCodes: '200',
//                consoleLogResponseBody: true
//        ).content.data
        String taskIdJsonString = ""
        pipelineContext.maskPasswords(varPasswordPairs: [[var: 'username'], [var: 'password']], varMaskRegexes: []) {
            taskIdJsonString = pipelineContext.sh(returnStdout: true, script: 'curl -s --request POST' +
                    ' --url \'https://' + proxmoxNode + '.zevrant-services.com:8006/api2/json/nodes/' + proxmoxNode + '/storage/vm-images/upload' + params + '\' '
                    + ' --header \'Authorization: PVEAPIToken=' + username + '=' + password + '\''
                    + ' --header \'Content-Type: multipart/form-data\''
                    + ' --header \'User-Agent: insomnia/11.6.1\''
                    + ' --form filename=@' + imagePath)
        }
        pipelineContext.println('output: ' + taskIdJsonString)
        def taskId = pipelineContext.readJSON(text: taskIdJsonString)
        if (!waitForTaskCompletion(proxmoxNode, taskId.data)) {
            throw new RuntimeException("Failed to upload image $imagePath to node $proxmoxNode")
        }
        ProxmoxVolume volume = this.listStoredVolumes(storageName, proxmoxNode)
                .find({ volume -> volume.volid.contains(getFilenameFromPath(imagePath)) })

        return volume
    }

    private static String getFilenameFromPath(String path) {
        String pathParts = path.split("/")
        return pathParts[pathParts.length() - 1]
    }

    private boolean waitForTaskCompletion(String proxmoxNode, String upid) {
        def response = this.pipelineContext.httpRequest(
                httpMode: 'GET',
                url: "${proxmoxUrl}/nodes/${proxmoxNode}/tasks/${URLEncoder.encode(upid, StandardCharsets.UTF_8)}/status",
                consoleLogResponseBody: true,
                customHeaders: [
                        [
                                'name'     : "Authorization",
                                'value'    : "PVEAPIToken=" + username + "=" + password,
                                'maskValue': true
                        ]
                ]
        )
        pipelineContext.println("Received task status, parsing results and waiting if needed")
        pipelineContext.println(response.content)
        def status = pipelineContext.readJSON(text: response.content).data
        pipelineContext.println(pipelineContext.writeJSON(returnText: true, json: status))
        pipelineContext.println(status)
        pipelineContext.println("Post print")
        while ("stopped" != status.status.toLowerCase()) {
            pipelineContext.println(status.status.toLowerCase())
            response = this.pipelineContext.httpRequest(
                    httpMode: 'GET',
                    url: "${proxmoxUrl}/nodes/${proxmoxNode}/tasks/${URLEncoder.encode(upid, StandardCharsets.UTF_8)}/status",
                    consoleLogResponseBody: true,
                    customHeaders: [
                            [
                                    'name'     : "Authorization",
                                    'value'    : "PVEAPIToken=" + username + "=" + password,
                                    'maskValue': true
                            ]
                    ]
            )
            status = pipelineContext.readJSON(text: response.content).data
        }
        return "ok" == status.exitstatus.toLowerCase()
    }

    public void deleteImage(String storageName, String proxmoxNode, String volumeId) {
        def response = this.pipelineContext.httpRequest(
                httpMode: 'DELETE',
                url: "${proxmoxUrl}/nodes/${proxmoxNode}/storage/${storageName}/content/${URLEncoder.encode(volumeId, StandardCharsets.UTF_8)}",
                consoleLogResponseBody: true,
                customHeaders: [
                        [
                                'name'     : "Authorization",
                                'value'    : "PVEAPIToken=" + username + "=" + password,
                                'maskValue': true
                        ]
                ]
        )
    }

    @NonCPS
    public List<ProxmoxVolume> sortByVersion(List<ProxmoxVolume> volumes) {
        volumes.sort {
            String versionString1 = volume1.volumeName.replace(".qcow2", "").replace(codeUnit.name, "").substring(1)
            String versionString2 = volume2.volumeName.replace(".qcow2", "").replace(codeUnit.name, "").substring(1)
            Version volumeVersion1 = new Version(versionString1)
            Version volumeVersion2 = new Version(versionString2)
            int major = volumeVersion2.major <=> volumeVersion1.major
            int minor = volumeVersion2.minor <=> volumeVersion1.minor
            int patch = volumeVersion2.patch <=> volumeVersion1.patch

            return major ?: minor ?: patch
        }
    }
}
