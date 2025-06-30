package com.zevrant.services.pojo

enum NotificationChannel {
    DISCORD_CICD('discord-webhook'),
    SLACK_TERRAFORM_DEPLOY('slack-tf-deploy')

    private String secretId

    private NotificationChannel(String secretId) {
        this.secretId = secretId
    }

    String getSecretId() {
        return secretId
    }
}