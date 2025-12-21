package com.zevrant.services.services

import com.zevrant.services.pojo.ProxmoxVolume

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
                method: 'GET',
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

        LinkedHashMap<String, Object> responseContent = pipelineContext.readJSON(text: response.content)
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

    public void uploadImage(String storageName, String proxmoxNode, String imagePath, String imageChecksum) {

        def parameters = [
                "content": "import",
                "checksum": imageChecksum,
                "checksum-algorithm": "sha512",
        ]
        String params = ""
        for (key in parameters.keySet()) {
            if (params == "") {
                params = "?${key}=${parameters[key]}"
            } else {
                params = "&${key}=${parameters[key]}"
            }

        }


        String taskId = pipelineContext.httpRequest(
                url: "${proxmoxUrl}/nodes/${proxmoxNode}/storage/${storageName}/upload${params}",
                httpMode: "POST",
                wrapAsMultipart: true,
                uploadFile: imagePath,
                multipartName: getFilenameFromPath(imagePath),
                requestBody: params,
                customHeaders: [
                        [
                                'name'     : "Authorization",
                                'value'    : "PVEAPIToken=" + username + "=" + password,
                                'maskValue': true
                        ],
                        [
                                'name' : 'CONTENT-TYPE',
                                'value': 'multipart/form-data'
                        ]
                ],
                validResponseCodes: '200',
                consoleLogResponseBody: true
        ).content.data

        if (!waitForTaskCompletion(proxmoxNode, taskId)) {
            throw new RuntimeException("Failed to upload image $imagePath to node $proxmoxNode")
        }
        ProxmoxVolume volume = this.listStoredVolumes(storageName, proxmoxNode)
                .find({ volume -> volume.volid.contains(getFilenameFromPath(imagePath)) })


    }

    private static String getFilenameFromPath(String path) {
        String pathParts = path.split("/")
        return pathParts[pathParts.length() - 1]
    }

    private boolean waitForTaskCompletion(String proxmoxNode, String upid) {
        def response = this.pipelineContext.httpRequest(
                method: 'GET',
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

        while ("stopped" != response.data.status) {
            this.pipelineContext.println(response.data.status)
            response = this.pipelineContext.httpRequest(
                    method: 'GET',
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
        }
        return "ok" == response.data.exitstatus.toLowerCase()
    }

    public void deleteImage(String storageName, String proxmoxNode, String volumeId) {
        def response = this.pipelineContext.httpRequest(
                method: 'DELETE',
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
}
