package com.zevrant.services.pojo

import com.zevrant.services.enumerations.SecretType

class SecretMapping {

    private final SecretType secretType;
    private final String secretPath;

    SecretMapping(SecretType secretType, String secretPath) {
        this.secretType = secretType
        this.secretPath = secretPath
    }

    SecretType getSecretType() {
        return secretType
    }

    String getSecretPath() {
        return secretPath
    }
}
