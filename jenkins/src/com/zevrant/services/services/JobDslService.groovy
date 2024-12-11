package com.zevrant.services.services

import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.codeunit.CodeUnit
import org.apache.commons.lang3.StringUtils

import static com.zevrant.services.enumerations.PipelineTriggerType.UPSTREAM

class JobDslService extends Service {
    private Object dslContext = pipelineContext //renaming variable for clarity of use
    JobDslService(Object pipelineContext) {
        super(pipelineContext)
    }

    void createPipeline(String folder, Pipeline pipeline) {

        if (StringUtils.isBlank(folder.trim()) && pipeline.name == 'container-build') {
            throw new RuntimeException("container build for ${pipeline.envs.repository}should not be in the root folder", )
        }
//        folder = (StringUtils.isBlank(folder)) ? "/" : folder
        folder = (folder.lastIndexOf('/') == folder.length() - 1 && folder.length() > 1) ? folder.substring(0, Math.max(0, folder.length() - 1)) : folder
        folder = (folder.length() == 0 || '/' == folder.charAt(0).toString())? folder : '/' + folder
        dslContext.println(folder + "/${pipeline.name}")
        dslContext.pipelineJob(folder + "/" + pipeline.name) {
            description pipeline.description

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

            displayName(pipeline.displayName)
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
                                        dslContext.println "WARN: Ignoring Generic trigger as it is not yet implemented"
                                        break
                                    case UPSTREAM:
                                        upstream {
                                            upstreamProjects(trigger.value)
                                        }
                                        break
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

                if (pipeline.disableResume) {
                    disableResume()
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

                            branch('master')

                            browser {
                                gitWeb("https://github.com:zevrant/zevrant-services-pipeline")
                            }
                            extensions {
                                cloneOptions {
                                    shallow(true)
                                    depth(1)
                                }
                                wipeOutWorkspace()
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
        String appFolderName = "/" + codeUnit.applicationType.value as String
        dslContext.folder(appFolderName) {

        }
        codeUnit.name.split("-").each { name -> jobName += name.capitalize() + " " }
        jobName = jobName.trim()
        String folderName = appFolderName + '/' + jobName.replaceAll(" ", "-").toLowerCase()
        dslContext.folder(folderName) {
            displayName(jobName)
        }

        dslContext.multibranchPipelineJob(folderName + '/' + jobName + '-multibranch') {
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
                                    url("${codeUnit.repo.getHttpsUri()}/${codeUnit.repo.getOrg()}/${codeUnit.repo.repoName}.git")
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
                        if (codeUnit.repo.httpsUri.contains('gitea')) {
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
                        } else {
                            github {
                                repoOwner(codeUnit.repo.org)
                                repository(codeUnit.repo.repoName)
                                repositoryUrl(codeUnit.repo.httpsUri)
                                configuredByUrl(true)
                                credentialsId(codeUnit.repo.credentialsId)
                                id(codeUnit.name)
                                traits {
                                    gitHubPullRequestDiscovery {
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
        }
        return folderName;
    }
}

