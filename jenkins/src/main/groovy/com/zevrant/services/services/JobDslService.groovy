package com.zevrant.services.services

import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.codeunit.CodeUnit

import static com.zevrant.services.enumerations.PipelineTriggerType.UPSTREAM

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
        folder = (folder.lastIndexOf('/') == folder.length() - 1) ? folder.substring(0, Math.max(0, folder.length() - 1)) : folder
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
                            pipeline.triggers.each { trigger ->
                                switch (trigger.type) {
                                    case PipelineTriggerType.CRON:
                                        cron {
                                            spec(trigger.value);
                                        }
                                        break;
                                    case PipelineTriggerType.GENERIC:
                                        println "WARN: Ignoring Generic trigger as it is not yet implemented"
                                        break
                                    case UPSTREAM:
                                    default:
                                        throw new RuntimeException("Pipeline Trigger Type Not Implemented ${trigger.type} for pipeline ${pipeline.name}")
                                }
                            }

                        }
                    }
                }
                if (!pipeline.allowConcurrency) {
                    disableConcurrentBuilds {
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

    String createMultibranch(CodeUnit codeUnit) {
        String jobName = ""
        dslContext.folder(codeUnit.applicationType.value) {

        }
        String folderName = codeUnit.applicationType.value + "/"
        codeUnit.name.split("-").each { name -> jobName += name.capitalize() + " " }
        jobName = jobName.trim()
        folderName += jobName + "/"
        dslContext.folder(folderName.substring(0, folderName.length() - 1)) {

        }

        dslContext.multibranchPipelineJob(folderName + codeUnit.name + "-multibranch") {
            displayName jobName + " Multibranch"
            factory {
                remoteJenkinsFileWorkflowBranchProjectFactory {
                    localMarker("")
                    matchBranches(false)
                    remoteJenkinsFile codeUnit.applicationType.getRemoteJenkinsfile()
                    remoteJenkinsFileSCM {
                        gitSCM {
                            branches {
                                branchSpec {
                                    name('main')
                                }
                            }
                            extensions {
                                wipeWorkspace()
                                cloneOption {
                                    shallow(true)
                                    depth(1)
                                    noTags(true)
                                    reference("")
                                    timeout(10)
                                }
                            }
                            userRemoteConfigs {
                                userRemoteConfig {
                                    name("Zevrant Services Pipeline") //Custom Repository Name or ID
                                    url("ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git")
                                    //URL for the repository
                                    refspec("main") // Branch spec
                                    credentialsId("jenkins-git") // Credential ID. Leave blank if not required
                                }
                                browser {} // Leave blank for default Git Browser
                                gitTool("") //Leave blank for default git executable
                            }
                        }
                    }
                }
            }
            branchSources {
                branchSource {
                    source {
                        giteaSCMSource {
                            serverUrl("https://${codeUnit.repo.hostName}")
                            repoOwner(codeUnit.repo.org)
                            repository(codeUnit.name)
                            credentialsId(codeUnit.repo.credentialsId)
                            id(codeUnit.name)
                            traits {
                                giteaPullRequestDiscovery {
                                    strategyId(0)
                                }
                                headWildcardFilter {
                                    includes('main PR-*')
                                    excludes('')
                                }
                                giteaBranchDiscovery {
                                    strategyId(3)
                                }
                                wipeWorkspaceTrait()
                            }
                        }
                    }
                }
            }
        }
        return folderName;
    }
}

