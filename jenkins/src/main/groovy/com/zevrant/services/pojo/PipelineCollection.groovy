package com.zevrant.services.pojo

import com.zevrant.services.PipelineTriggerType
import com.zevrant.services.enumerations.DefaultPipelineParameters
import com.zevrant.services.enumerations.TriggerVariableType

class PipelineCollection {

    static ArrayList<Pipeline> pipelines = new ArrayList<Pipeline>([
            new Pipeline(
                    name: "spring-kubernetes-build",
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
                    name: "androidBuild",
                    description: "Pipeline to build android apps",
                    parameters: new ArrayList<PipelineParameter>(
                            [
                                    DefaultPipelineParameters.BRANCH_PARAMETER.parameter,
                                    DefaultPipelineParameters.REPOSITORY_PARAMETER.parameter
                            ]
                    ),
                    jenkinsfileLocation: "jenkins/pipelines/androidBuild.groovy",
                    credentialId: "jenkins-git",
                    triggers: new ArrayList<>([
                            new PipelineTrigger(
                                    type: PipelineTriggerType.GENERIC,
                                    token: 'dwarfEcKRZNYbcRwAX2B8b2V3',
                                    parameters: new ArrayList<>([
                                            DefaultPipelineParameters.BRANCH_PARAMETER.parameter,
                                            DefaultPipelineParameters.REPOSITORY_PARAMETER.parameter
                                    ]),
                                    variables: new ArrayList<>([
                                            new GenericPipelineTriggerVariable(
                                                    key: "BRANCH_NAME",
                                                    expressionValue: "\$.ref",
                                                    triggerVariableType: TriggerVariableType.JSONPATH
                                            ),
                                            new GenericPipelineTriggerVariable(
                                                    key: "REPOSITORY",
                                                    expressionValue: "\$.repository.name",
                                                    triggerVariableType: TriggerVariableType.JSONPATH
                                            )
                                    ])
                            )
                    ])
            ),
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
            )
    ])
}
