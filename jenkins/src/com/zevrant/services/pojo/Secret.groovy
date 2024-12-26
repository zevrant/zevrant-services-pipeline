package com.zevrant.services.pojo

class Secret {

    private final String value

    Secret(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
