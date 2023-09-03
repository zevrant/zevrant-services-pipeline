package com.zevrant.services.pojo

import java.time.ZonedDateTime

class CertRotationInfo {

    private String secretName
    private ZonedDateTime start
    private ZonedDateTime expiration

    CertRotationInfo() {
    }

    CertRotationInfo(String secretName, ZonedDateTime start, ZonedDateTime expiration) {
        this.secretName = secretName
        this.start = start
        this.expiration = expiration
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
}
