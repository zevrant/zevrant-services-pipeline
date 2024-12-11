package com.zevrant.services.services

import com.zevrant.services.pojo.ProviderShasumsLinks
import org.apache.commons.lang.StringUtils
import org.json.JSONArray
import org.json.JSONObject

import java.nio.charset.StandardCharsets

class TerraformCloudService extends Service {

    private static final String baseUrl = "https://app.terraform.io/api/v2"

    TerraformCloudService(Object pipelineContext) {
        super(pipelineContext)
    }

    private Map<String, Object> getAuthHeader(String token) {
        return [
                'name' : "Authorization",
                'value': "bearer " + token.replace('"', ''),
                'maskValue': true
        ]
    }

    String getLatestGPGKeyId(String org, String token) {
        if (StringUtils.isBlank(org)) {
            throw new RuntimeException(('Org Must be provided for terraform cloud actions'))
        }
        Map<String,Object> authHeader = getAuthHeader(token)
        if (authHeader == null) {
            throw new RuntimeException(('auth header ended up being null'))
        }
        if (StringUtils.isBlank(authHeader['name'])) {
            throw new RuntimeException(('auth header name ended up being null'))
        }
        String encodedOrg = URLEncoder.encode(org, StandardCharsets.UTF_8)
        def httpResponse = pipelineContext.httpRequest(
                url: "https://app.terraform.io/api/registry/private/v2/gpg-keys?filter%5Bnamespace%5D=$encodedOrg",
                customHeaders: [
                        authHeader,
                        [
                                "name": 'Content-Type',
                                'value': 'application/vnd.api+json'
                        ]
                ],
                httpMode: "GET",
                ignoreSslErrors: true,
                validResponseCodes: "200",
                consoleLogResponseBody: false
        )

        JSONObject responseBody = pipelineContext.readJSON(text: httpResponse.content)

        int index = 0

        JSONArray keys = responseBody.data
        for (int i = 0; i < keys.length(); i++) {
            JSONObject gpgKey = keys.get(i) as JSONObject

            index = (keys[index].id == Math.max(Integer.parseInt(keys.get(index).id), Integer.parseInt(gpgKey.get("id") as String)))
                    ? index
                    : i
            return keys[index].get('attributes').get('key-id')
        }

    }

    ProviderShasumsLinks createProviderVersion(String gpgKeyId, String token, String orgName, String providerName, String version) {
        String encodedOrgName = URLEncoder.encode(orgName, StandardCharsets.UTF_8)
        String encodedProviderName = URLEncoder.encode(providerName, StandardCharsets.UTF_8)

        Map<String, Object> requestBody = [
                "data": [
                        "type"      : "registry-provider-versions",
                        "attributes": [
                                "version"  : version,
                                "key-id"   : gpgKeyId,
                                "protocols": ["6.0"]
                        ]
                ]
        ]
        pipelineContext.println(pipelineContext.writeJSON(json: requestBody, returnText: true))
        pipelineContext.println(pipelineContext.writeJSON(json: getAuthHeader(token), returnText: true))
        def httpResponse = pipelineContext.httpRequest(
                url: "${baseUrl}/organizations/$encodedOrgName/registry-providers/private/$encodedOrgName/$encodedProviderName/versions",
                customHeaders: [
                        getAuthHeader(token),
                        [
                                "name": 'Content-Type',
                                'value': 'application/vnd.api+json'
                        ]
                ],
                httpMode: "POST",
                ignoreSslErrors: true,
                validResponseCodes: "201",
                requestBody: pipelineContext.writeJSON(json: requestBody, returnText: true),
                consoleLogResponseBody: true
        )

        def responseBody = pipelineContext.readJSON(text: httpResponse.content)
        return new ProviderShasumsLinks(responseBody.data.links['shasums-upload'] as String, responseBody.data.links['shasums-sig-upload'] as String)
    }

    void uploadFile(String pathToFile, String token, String uploadUrl) {
        pipelineContext.httpRequest(
                url: uploadUrl,
                httpMode: "PUT",
                wrapAsMultipart: false,
                uploadFile: pathToFile,
                customHeaders: [
                        getAuthHeader(token),
                ],
                validResponseCodes: '200',
                consoleLogResponseBody: true
        )
    }

    String createProviderPlatform(String token, String orgName, String providerName, String version, TerraformCloudProviderBinary binary) {
        String encodedOrgName = URLEncoder.encode(orgName, StandardCharsets.UTF_8)
        String encodedProviderName = URLEncoder.encode(providerName, StandardCharsets.UTF_8)
        String encodedVersion = URLEncoder.encode(version, StandardCharsets.UTF_8)

        Map <String, Object> requestBody = [
                "data": [
                        "type": "registry-provider-version-platforms",
                        "attributes": [
                                "os": binary.os,
                                "arch": binary.arch,
                                "shasum": binary.shasum,
                                "filename": binary.fileName
                        ]
                ]
        ]

        def httpResponse = pipelineContext.httpRequest(
                url: "${baseUrl}/organizations/${encodedOrgName}/registry-providers/private/${encodedOrgName}/${encodedProviderName}/versions/${encodedVersion}/platforms",
                customHeaders: [
                        getAuthHeader(token),
                        [
                                "name": 'Content-Type',
                                'value': 'application/vnd.api+json'
                        ]
                ],
                httpMode: "POST",
                ignoreSslErrors: true,
                validResponseCodes: "201",
                requestBody: pipelineContext.writeJSON(json: requestBody, returnText: true),
                consoleLogResponseBody: true
        )

        def responseBody = pipelineContext.readJSON(text: httpResponse.content)
        return responseBody.data.links['provider-binary-upload']
    }
}
