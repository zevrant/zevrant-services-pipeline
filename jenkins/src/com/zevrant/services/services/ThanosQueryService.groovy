package com.zevrant.services.services

import CertRotationInfo
import Service
import DateTimeUtils
import JSONObject

import java.time.ZoneId
import java.time.ZonedDateTime

class ThanosQueryService extends Service {

    private final String thanosUri = 'http://thanos-query.monitoring.svc.cluster.local:9090/api/v1/'

    ThanosQueryService(Object pipelineContext) {
        super(pipelineContext)
    }

    private String queryThanos(String query) {
        def response = pipelineContext.httpRequest(
                httpMode: 'POST',
                url: "${thanosUri}query",
                consoleLogResponseBody: false,
                contentType: 'APPLICATION_FORM',
                requestBody: query
        )
        return response.content
    }

    List<CertRotationInfo> getServicesNeedingCertRotation() {
        String getCertExpiryQuery = 'query=x509_cert_not_after'
        List<CertRotationInfo> certRotationList = []
        JSONObject response = pipelineContext.readJSON(text: queryThanos(getCertExpiryQuery))
        response.data.result
                .findAll({ result ->
                    result.metric.get('issuer_CN') != 'ISRG Root X1' && result.metric.get('issuer_CN').contains('Zevrant Services')
                })
                .each({ result ->
                    String secretName = result.metric.secret_name
                    ZonedDateTime startTime = DateTimeUtils.unixToLocalDateTime((long) result.value[0])
                    ZonedDateTime expirationTime = DateTimeUtils.unixToLocalDateTime(Long.parseLong(result.value[1] as String))

                    if(ZonedDateTime.now(ZoneId.of("GMT-4")).plusMinutes(58).isAfter(expirationTime)) {
                        certRotationList.add(new CertRotationInfo(secretName, startTime, expirationTime))
                    }

                })
        return certRotationList
    }

}