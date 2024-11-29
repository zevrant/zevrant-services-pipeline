package com.zevrant.services.services


import NotificationChannel

class NotificationService extends Service {

    private static final String failureColor = 'bb0000'
    private static final String successColor = '00dd00'


    NotificationService(def pipelineContext) {
        super(pipelineContext)
    }


    void sendDiscordNotification(String message, String link = '', String result = "SUCCESS", String title, NotificationChannel channel) {
        if(pipelineContext.env.sandboxMode != '' && pipelineContext.env.sandboxMode != null) {
            pipelineContext.println('Not sending notification because sandbox mode is enabled')
            return
        }
        pipelineContext.withCredentials([pipelineContext.string(credentialsId: channel.getSecretId(), variable: 'url')]) {
            pipelineContext.discordSend description: message, link: link, result: result, title: title, webhookURL: pipelineContext.url
        }
    }

}