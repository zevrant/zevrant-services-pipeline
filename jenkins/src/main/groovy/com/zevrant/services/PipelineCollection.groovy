package com.zevrant.services

class PipelineCollection {

    static ArrayList<Pipeline> pipelines = new ArrayList<Pipeline>([
            new Pipeline(
                    name: "spring-kubernetes-build-job",
                    description: "Pipeline in charge of building microservices intended for deployment onto the kubernetes cluster",
                    parameters: new ArrayList<PipelineParameter>(
                            [
                                    DefaultPipelineParameters.BRANCH_PARAMETER.parameter,
                                    DefaultPipelineParameters.REPOSITORY_PARAMETER.parameter
                            ]
                    ),
                    jenkinsfileLocation: "jenkins/pipelines/build.groovy",
                    credentialId: "jenkins-git"
            ),
            new Pipeline(
                    name: "android-build-job",
                    description: "Pipeline to build android apps",
                    parameters: new ArrayList<PipelineParameter>(
                            [
                                    DefaultPipelineParameters.BRANCH_PARAMETER.parameter,
                                    DefaultPipelineParameters.REPOSITORY_PARAMETER.parameter
                            ]
                    ),
                    jenkinsfileLocation: "jenkins/pipelines/build.groovy",
                    credentialId: "jenkins-git"
            ),
            new Pipeline(
                    name: "base-image-build-job",
                    description: "Pipeline to build base docker images",
                    jenkinsfileLocation: "jenkins/pipelines/buildBaseImages.groovy",
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(PipelineTriggerType.CRON, "0 3 * * 6")
                            ])
            )
    ])
}
