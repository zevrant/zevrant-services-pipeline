import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.PipelineParameter
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.PipelineCollection
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.pojo.PipelineTrigger
import com.zevrant.services.pojo.AndroidCodeUnit
import com.zevrant.services.pojo.LibraryCodeUnit
import com.zevrant.services.pojo.LibraryCodeUnitCollection
import com.zevrant.services.pojo.AndroidCodeUnitCollection
import com.zevrant.services.pojo.SpringCodeUnitCollection
import com.zevrant.services.pojo.SpringCodeUnit
import com.zevrant.services.pojo.CodeUnit

LibraryCodeUnitCollection.libraries.each { libraryCodeUnit ->
    createMultibranch((CodeUnit) libraryCodeUnit)
}

SpringCodeUnitCollection.microservices.each { springCodeUnit ->
    String folder = createMultibranch(springCodeUnit as CodeUnit)
    Pipeline developDeployPipeline = new Pipeline(
            name: "${springCodeUnit.name}-deploy-to-develop",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: springCodeUnit.repo.getSshUri(),
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : springCodeUnit.name,
                    'ENVIRONMENT': 'develop'
            ]),
            triggers           : [
                    new PipelineTrigger([
                            type : PipelineTriggerType.CRON,
                            value: "H */11 * * *"
                    ])
            ],
    )
    Pipeline prodDeployPipeline = new Pipeline(
            name: "${springCodeUnit}-deploy-to-prod",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: springCodeUnit.repo.getSshUri(),
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : springCodeUnit.name,
                    'ENVIRONMENT': 'prod'
            ]),
            triggers           : [
                    new PipelineTrigger([
                            type : PipelineTriggerType.CRON,
                            value: "H */11 * * *"
                    ])
            ],
    )
    createPipeline(folder, developDeployPipeline);
    if(springCodeUnit.prodReady) {
        createPipeline(folder, prodDeployPipeline);
    }
}

AndroidCodeUnitCollection.androidApps.each( { androidCodeUnit ->
    String androidFolder = createMultibranch(androidCodeUnit as CodeUnit)

    Pipeline androidDevelopDeployPipeline = new Pipeline(
            name: "Zevrant-Android-App-Release-To-Internal-Testing",
            parameters: new ArrayList<>([
            ]),
            gitRepo: "ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/android-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : 'zevrant-android-app',
                    'ENVIRONMENT': 'develop'
            ])
    )
    Pipeline androidProdDeployPipeline = new Pipeline(
            name: "Zevrant-Android-App-Release-To-Production",
            parameters: new ArrayList<>([
            ]),
            gitRepo: "ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/android-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : 'zevrant-android-app',
                    'ENVIRONMENT': 'prod'
            ])
    )
    createPipeline(androidFolder, androidDevelopDeployPipeline)
    createPipeline(androidFolder, androidProdDeployPipeline)
})

String adminFolder = createMultibranch('jenkins-cac', ApplicationType.ADMIN_UTILITIES, 'jenkins/pipelines/admin/cacUpdate.groovy')

String createMultibranch(CodeUnit codeUnit) {
    String jobName = ""
    folder(codeUnit.applicationType.value) {

    }
    String folderName = codeUnit.applicationType.value + "/"
    codeUnit.name.split("-").each { name -> jobName += name.capitalize() + " " }
    jobName = jobName.trim()
    folderName += jobName + "/"
    folder(folderName.substring(0, folderName.length() - 1)) {

    }

    multibranchPipelineJob(folderName + codeUnit.name + "-multibranch") {
        displayName jobName + " Multibranch"
        factory {
            remoteJenkinsFileWorkflowBranchProjectFactory {
                localMarker("")
                matchBranches(false)
                remoteJenkinsFileSCM {
                    gitSCM {
                        branches {
                            branchSpec {
                                name('master')
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
                                url("ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/zevrant-services-pipeline.git")
                                //URL for the repository
                                refspec("master") // Branch spec
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
//            git {
//                id(codeUnit.name) // IMPORTANT: use a constant and unique identifier
//                remote("ssh://git@ssh.gitea.zevrant-services.com:30121/zevrant-services/${codeUnit.name}.git")
//                credentialsId 'jenkins-git-access-token'
//                checkoutCredentialsId 'jenkins-git'
//            }
            branchSource {
                source {
                    giteaSCMSource {
                        serverUrl(codeUnit.repo.getHttpsUri())
                        repoOwner(codeUnit.repo.org)
                        repository(codeUnit.name)
                        credentialsId(codeUnit.repo.credentialsId)
                        id(codeUnit.name)
                        traits {
                            giteaPullRequestDiscovery {
                                strategyId(0)
                            }
                            headWildcardFilter {
                                includes('master PR-*')
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

(PipelineCollection.pipelines as List<Pipeline>).each { pipeline ->
    createPipeline("", pipeline)
}


String kubernetesServicesFolder = 'kubernetes-services'
folder(kubernetesServicesFolder) {
    displayName = 'Kubernetes Services'
}

KubernetesServiceCollection.services.each { kubernetesService ->
    kubernetesService.environments.each { environment ->
        Pipeline pipeline = new Pipeline([
                name               : kubernetesService.serviceName.capitalize(),
                triggers           : [
                        new PipelineTrigger([
                                type : PipelineTriggerType.CRON,
                                value: "H */11 * * *"
                        ])
                ],
                envs               : [
                        ENVIRONMENT : environment.getNamespaceName(),
                        SERVICE_NAME: kubernetesService.serviceName
                ],
                jenkinsfileLocation: 'jenkins/pipelines/serviceDeploy.groovy'
        ])
        createPipeline(kubernetesServicesFolder.concat('/'), pipeline)
    }
}

/**
 *
 * @param folder must contain ending / or be empty string
 * @param pipeline
 */
void createPipeline(String folder, Pipeline pipeline) {
    if (folder == null) {
        folder = ""
    }
    pipelineJob(folder + pipeline.name) {
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

                        branch('master')

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
