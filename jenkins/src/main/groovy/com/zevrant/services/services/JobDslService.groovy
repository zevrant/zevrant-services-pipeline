package com.zevrant.services.services

import com.zevrant.services.pojo.Pipeline

class JobDslService extends Service {
    private Object dslContext = pipelineContext //renaming variable for clarity of use
    JobDslService(Object pipelineContext) {
        super(pipelineContext)
    }

    /**
     *
     * @param folder must contain ending / or be empty string
     * @param pipeline
     */
    void createPipeline(String folder, Pipeline pipeline) {
        folder = (folder == null) ? "" : folder
        folder = (folder.lastIndexOf('/') == folder.length() - 1)? folder.substring(0, Math.max(0, folder.length() - 1)): folder
        dslContext.pipelineJob(folder + "/" + pipeline.name) {
            description pipeline.description
            String jobDisplayName = ""
            pipeline.name.split("-").each { piece ->
                jobDisplayName += piece.capitalize() + " "
            }

            if (pipeline.triggers != null && !pipeline.triggers.isEmpty()) {
                pipeline.triggers.each { trigger ->
                    if (trigger.type == PipelineTriggerType.GENERIC) {
                        genericTrigger {
                            genericVariables {
                                if (trigger.variables != null && !trigger.variables.isEmpty()) {
                                    trigger.variables.each { variable ->
                                        genericVariable {
                                            key(variable.key)
                                            value(variable.expressionValue)
                                            expressionType(variable.triggerVariableType.value)
                                            defaultValue(variable.defaultValue) //Optional, defaults to empty string
                                        }
                                    }
                                }
                            }
                            token(trigger.token)
                        }
                    }
                }
            }

            if (pipeline.envs != null
                    && !pipeline.envs.isEmpty()) {
                environmentVariables {
                    pipeline.envs.keySet().each { key ->
                        env(key, pipeline.envs.get(key))
                    }
                }
            }

            displayName(jobDisplayName.trim())
            disabled pipeline.disabled
            logRotator {
                numToKeep pipeline.buildsToKeep
            }

            if (pipeline.parameters != null && pipeline.parameters.size() > 0) {
                parameters() {
                    pipeline.parameters.each { parameter ->
                        switch (parameter.type) {
                            case String.class:
                                stringParam(parameter.name, parameter.defaultValue, parameter.description)
                                break;
                            case Boolean.class:
                                booleanParam(parameter.name, parameter.defaultValue, parameter.description)
                                break;
                            case List.class:
                                choiceParam(parameter.name, parameter.defaultValue, parameter.description)
                                break;
                            default:
                                throw RuntimeException("Parameter not supported")
                        }
                    }
                }
            }
            properties {
                if (pipeline.triggers.size() > 0) {
                    pipelineTriggers {
                        triggers {
                            cron {
                                pipeline.triggers.each { trigger ->
                                    switch (trigger.type) {
                                        case PipelineTriggerType.CRON:
                                            spec(trigger.value);
                                            break;
                                        case PipelineTriggerType.GENERIC:
                                            println "WARN: Ignoring Generic trigger as it is not yet implemented"
                                            break
                                        default:
                                            throw new RuntimeException("Pipeline Trigger Type Not Implemented ${trigger.type} for pipeline ${pipeline.name}")
                                    }
                                }
                            }

                        }
                    }
                }
                if(!pipeline.allowConcurrency) {
                    disableConcurrentBuilds{
                        abortPrevious(false)
                    }
                }
            }
            definition {
                cpsScm {
                    lightweight(false)
                    scm {
                        git {
                            remote {
                                credentials(pipeline.credentialId)
                                name('origin')
                                url(pipeline.gitRepo)
                            }

                            branch('main')

                            browser {
                                gitWeb("https://github.com:zevrant/zevrant-services-pipeline")
                            }
                            extensions {
                                cloneOptions {
                                    shallow(true)
                                    depth(1)
                                }
                            }
                        }
                    }
                    scriptPath(pipeline.jenkinsfileLocation)
                }
            }
        }
    }

}
