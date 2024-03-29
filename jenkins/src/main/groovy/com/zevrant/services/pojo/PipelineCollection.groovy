package com.zevrant.services.pojo


import com.zevrant.services.enumerations.PipelineTriggerType

class PipelineCollection {

    static ArrayList<Pipeline> pipelines = new ArrayList<Pipeline>([
            new Pipeline(
                    name: "base-image-build",
                    description: "Pipeline to build base docker images",
                    jenkinsfileLocation: "jenkins/pipelines/buildContainerImages.groovy",
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
                    jenkinsfileLocation: 'jenkins/pipelines/podCleanup.groovy',
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "34 * * * *"
                            )
                    ])
            ),
            new Pipeline(
                    name: 'Dynamic Dns',
                    description: 'Pushes chenges needed to maintain Dynamic DNS',
                    jenkinsfileLocation: 'jenkins/pipelines/dynamic-dns.groovy',
                    credentialId: 'jenkins-git',
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "*/4 * * * *"
                            )
                    ])
            ),
            new Pipeline(
                    name: 'Certificate Authority Generator',
                    description: 'Creates & signs certificates for subordinate CA running in kubernetes',
                    jenkinsfileLocation: 'jenkins/pipelines/certificate-authority-generator.groovy',
                    credentialId: 'jenkins-git',
                    parameters: [
                            new PipelineParameter<String>(String, 'ENVIRONMENT', 'environment being setup', null),
                            new PipelineParameter<Boolean>(Boolean, 'REMOVE_EXISTING', 'If enabled, remove existing ca install before initializing', false)
                    ]
            ),
            new Pipeline(
                    name: 'DNS Reload',
                    description: 'restarts dns servers to load new configuration',
                    jenkinsfileLocation: 'jenkins/pipelines/admin/dnsReload.groovy',
                    credentialId: 'jenkins-git'
            ),
            new Pipeline(
                    name: 'Create UI Certificate',
                    description: 'Requests Let\'s Encrypt Certificates using DNS-01 auth and puts them into a kubernetes secret',
                    jenkinsfileLocation: 'jenkins/pipelines/admin/create-ui-certificate.groovy',
                    credentialsId: 'jenkins-git',
                    parameters: [
                            new PipelineParameter<String>(String, 'ENVIRONMENT', 'environment the UI is deployed into', null),
                            new PipelineParameter<String>(String, 'DOMAIN', 'Domain name the certificate is being requested for', null)
                    ]
            )
    ])
}
