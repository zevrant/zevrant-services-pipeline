package com.zevrant.services.pojo

class AppEdit {

    private String id;
    private String expiryTimeSeconds;

    AppEdit(String id, String expiryTimeSeconds) {
        this.id = id
        this.expiryTimeSeconds = expiryTimeSeconds
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    String getExpiryTimeSeconds() {
        return expiryTimeSeconds
    }

    void setExpiryTimeSeconds(String expiryTimeSeconds) {
        this.expiryTimeSeconds = expiryTimeSeconds
    }
}
