package com.zevrant.services.pojo

enum NotificationChannel {
    DISCORD_CICD('discord-webhook')

    private String secretId

    private NotificationChannel(String secretId) {
        this.secretId = secretId
    }

    String getSecretId() {
        return this.secretId
    }
}