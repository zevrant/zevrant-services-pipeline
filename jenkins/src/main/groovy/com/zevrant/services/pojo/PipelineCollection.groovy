package com.zevrant.services.pojo


import com.zevrant.services.enumerations.PipelineTriggerType

class PipelineCollection {

    static ArrayList<Pipeline> pipelines = new ArrayList<Pipeline>([
            new Pipeline(
                    name: "base-image-build",
                    description: "Pipeline to build base docker images",
                    jenkinsfileLocation: "jenkins/pipelines/containers/buildContainerImages.groovy",
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "0 3 * * 6")
                    ]),
                    envs: [
                            FOLDER_PATH: 'containers'
                    ]
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
            ),
            new Pipeline(
                    name: 'restart-jenkins',
                    folder: 'Admin Utilities',
                    description: 'Pipeline intended to restart jenkins when it is time to reload certificates',
                    credentialId: 'jenkins-git',
                    jenkinsfileLocation: 'jenkins/pipelines/admin/jenkins-restart.groovy',
                    disableResume: false,
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "H */9 * * *"
                            )
                    ])
            ),
            new Pipeline(
                    name: 'Check Certs',
                    description: "Checks certificates on managed services using ephemeral certs",
                    credentialsId: 'jenkins-git',
                    jenkinsfileLocation: 'jenkins/pipelines/cert-rotation.groovy',
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.CRON,
                                    value: "H */1 * * *"
                            )
                    ])
            )
    ])
}
