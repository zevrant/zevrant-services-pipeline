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
                method: 'POST',
                customeHeaders: [
                        [
                                name : 'Content-Type',
                                value: 'application/x-www-form-urlencoded',
                        ]
                ],
                requestBody: 'client_id=' + encodedClientId + '&client_secret=' + encodedClientSecret + '&grant_type=client_credentials&audience=https://api.hashicorp.cloud',
                validResponseCodes: '200,201'
        )

        return pipelineContext.readJSON(test: response.content).access_token
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
                method: 'GET',
                customeHeaders: [
                        [
                                name : 'Authorization',
                                value: 'Bearer ' + hcpToken
                        ]
                ],
                validResponseCodes: '200'
        )

        pipelineContext.println(response.content)

        return new Secret('', '')
    }

}
