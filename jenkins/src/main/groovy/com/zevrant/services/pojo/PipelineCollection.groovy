package com.zevrant.services.pojo


import com.zevrant.services.enumerations.PipelineTriggerType

class PipelineCollection {

    static ArrayList<Pipeline> pipelines = new ArrayList<Pipeline>([
            new Pipeline(
                    name: "base-image-build",
                    description: "Pipeline to build base docker images",
                    jenkinsfileLocation: "jenkins/pipelines/buildBaseImages.groovy",
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "0 3 * * 6")
                    ])
            ),
            new Pipeline(
                    name: 'Pod cleanup',
                    description: "Terminated and failed or evicted pods",
                    jenkinsfileLocation: 'jenkins/pipelines/deleteEvictions.groovy',
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "34 * * * *"
                            )
                    ])
            )
    ])
}
