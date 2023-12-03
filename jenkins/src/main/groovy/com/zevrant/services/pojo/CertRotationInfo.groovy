package com.zevrant.services.pojo

import java.time.ZonedDateTime

class CertRotationInfo {

    private String secretName
    private ZonedDateTime start
    private ZonedDateTime expiration
    private String namespace

    CertRotationInfo() {
    }

    CertRotationInfo(String secretName, ZonedDateTime start, ZonedDateTime expiration, namespace = '') {
        this.secretName = secretName
        this.start = start
        this.expiration = expiration
        this.namespace = namespace
    }

    String getSecretName() {
        return secretName
    }

    void setSecretName(String secretName) {
        this.secretName = secretName
    }

    ZonedDateTime getStart() {
        return start
    }

    void setStart(ZonedDateTime start) {
        this.start = start
    }

    ZonedDateTime getExpiration() {
        return expiration
    }

    void setExpiration(ZonedDateTime expiration) {
        this.expiration = expiration
    }

    String getNamespace() {
        return namespace
    }
}
