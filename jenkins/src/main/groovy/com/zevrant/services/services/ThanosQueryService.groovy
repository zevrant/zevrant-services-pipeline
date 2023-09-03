package com.zevrant.services.services

import com.zevrant.services.services.Service

class ThanosQueryService extends Service {

    private final String thanosUri = 'http://thanos-query.monitoring.svc.cluster.local:9090/api/v1/'

    ThanosQueryService(Object pipelineContext) {
        super(pipelineContext)
    }

    private void queryThanos(String query) {
        def response = pipelineContext.httpRequest(
                httpMode: 'POST',
                url: "${thanosUri}query",
                consoleLogResponseBody: true,
                contentType: 'APPLICATION_FORM_DATA',
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