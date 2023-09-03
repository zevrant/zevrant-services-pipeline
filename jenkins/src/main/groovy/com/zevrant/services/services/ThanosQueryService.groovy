package com.zevrant.services.services

import com.zevrant.services.services.Service

class ThanosQueryService extends Service {

    private final String thanosUri = 'http://thanos-query.monitoring.svc.cluster.local/api/v1/'
    String oauthFormBody = "grant_type=password&username=${URLEncoder.encode(pipelineContext.username as String, StandardCharsets.UTF_8)}" + "&password=${URLEncoder.encode(pipelineContext.password as String, StandardCharsets.UTF_8)}"
    ThanosQueryService(Object pipelineContext) {
        super(pipelineContext)
    }

    private void queryThanos(String query) {
        def response = httpRequest(
                httpMode: 'GET',
                url: "${thanosUri}/query",
                consoleLogResponseBudt: true,
                requestBody: query
        )
        return response.content
    }

    List<String> getServicesNeedingCertRotation() {
        String getCertExpiryQuery = 'query=x509_cert_not_after'

        queryThanos(getCertExpiryQuery)

        return new ArrayList<String>()
    }

}