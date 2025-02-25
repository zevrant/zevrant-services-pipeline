package com.zevrant.services.pojo

import com.zevrant.services.enumerations.SecretType

class SecretMapping {

    private final SecretType secretType;
    private final String secretPath;
    private final boolean stripPrefix;

    SecretMapping(SecretType secretType, String secretPath, boolean stripPrefix = false) {
        this.secretType = secretType
        this.secretPath = secretPath
        this.stripPrefix = stripPrefix
    }

    SecretType getSecretType() {
        return secretType
    }

    String getSecretPath() {
        return secretPath
    }

    boolean getStripPrefix() {
        return stripPrefix
    }
}
