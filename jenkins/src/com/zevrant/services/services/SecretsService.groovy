package com.zevrant.services.services

import com.zevrant.services.pojo.Secret

import java.nio.charset.StandardCharsets

class SecretsService extends Service {

    SecretsService(Object pipelineContext) {
        super(pipelineContext)
    }

    String getHcpApiToken(String clientId, String clientSecret) {
        String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8)
        String encodedClientSecret = URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
        def response = pipelineContext.httpRequest(
                url: 'https://auth.idp.hashicorp.com/oauth2/token',
                httpMode: 'POST',
                customHeaders: [
                        [
                                name : 'Content-Type',
                                value: 'application/x-www-form-urlencoded',
                        ]
                ],
                requestBody: 'client_id=' + encodedClientId + '&client_secret=' + encodedClientSecret + '&grant_type=client_credentials&audience=https://api.hashicorp.cloud',
                validResponseCodes: '200,201',
        )

        return pipelineContext.readJSON(text: response.content).access_token
    }

    Secret getSecret(String secretName, String hcpToken, useCloud = false, String orgId = '', String projectId = '') {
        String getSecretUrl = ''
        if (useCloud) {
            getSecretUrl = 'https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/8a8ed772-fcf9-4a4b-b70f-bfa8554dd909/projects/3cdbcc02-4119-4937-81d6-6009254f3b42/apps/sample-app/secrets/' + secretName
        } else if (StringUtils.isBlank(orgId)) {
            throw new RuntimeException("When not using cloud Vault, the organization id must be provided")
        } else if (StringUtils.isBlank(projectId)) {
            throw new RuntimeException("When not using cloud Vault, the project id must be provided")
        }


        def response = pipelineContext.httpRequest(
                url: getSecretUrl,
                httpMode: 'GET',
                customHeaders: [
                        [
                                name : 'Authorization',
                                value: 'Bearer ' + hcpToken,
                                mask : true
                        ]
                ],
                validResponseCodes: '200'
        )

        def jsonObject = pipelineContext.readJSON(text: response.content)

        response = pipelineContext.httpRequest(
                url: "${getSecretUrl}/versions/${jsonObject.secret.latest_version}:open",
                httpMode: 'GET',
                customHeaders: [
                        [
                                name : 'Authorization',
                                value: 'Bearer ' + hcpToken,
                                mask : true
                        ]
                ],
                validResponseCodes: '200'
        )

        jsonObject = pipelineContext.readJSON(text: response.content)

        return new Secret(jsonObject.static_version.value)
    }

    String getLocalApiToken(String clientId, String clientSecret) {
        String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8)
        def response = pipelineContext.httpRequest(
                url: 'https://vault.zevrant-services.com/v1/auth/userpass/login/' + encodedClientId,
                httpMode: 'POST',
                customHeaders: [
                        [
                                name : 'Content-Type',
                                value: 'application/json',
                        ]
                ],
                requestBody: '{"password":"' + clientSecret + '"}',
                validResponseCodes: '200',
                ignoreSslErrors: true
        )

        return pipelineContext.readJSON(text: response.content).auth.client_token
    }

    Map<String, Object> getLocalSecret(String authToken, String secretPath) {
        def response = pipelineContext.httpRequest(
                url: "https://vault.zevrant-services.com/v1/kv/data/${secretPath}",
                httpMode: 'GET',
                customHeaders: [
                        [
                                name : 'Content-Type',
                                value: 'application/json',
                        ],
                        [
                                name : 'X-Vault-Token',
                                value: authToken,
                                mask : true
                        ]
                ],
                ignoreSslErrors: true,
                validResponseCodes: '200',
        )

        return pipelineContext.readJSON(text: response.content).data.data
    }
}
