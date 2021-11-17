import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.DefaultPipelineParameters
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.PipelineParameter
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.PipelineCollection

(libraryRepositories as List<String>).each { libraryRepository ->
    String folder = createMultibranch(libraryRepository, ApplicationType.LIBRARY)
    Pipeline pipeline = new Pipeline(
            name: libraryRepository,
            parameters: new ArrayList<>([
                    DefaultPipelineParameters.BRANCH_PARAMETER.getParameter()
            ]),
            gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/libraryBuild.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY': libraryRepository,
            ])
    );
    createPipeline(folder, pipeline);
}

(microserviceRepositories as List<String>).each { microserviceRepository ->
    String folder = createMultibranch(microserviceRepository, ApplicationType.SPRING)
//    Pipeline pipeline = new Pipeline(
//            name: microserviceRepository,
//            parameters: new ArrayList<>([
//                    DefaultPipelineParameters.BRANCH_PARAMETER.getParameter()
//            ]),
//            gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
//            jenkinsfileLocation: 'jenkins/pipelines/spring-build.groovy',
//            credentialId: 'jenkins-git',
//            envs: new HashMap<>([
//                    'REPOSITORY': microserviceRepository,
//            ])
//    );
    Pipeline developDeployPipeline = new Pipeline(
            name: "${microserviceRepository}-deploy-to-develop",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : microserviceRepository,
                    'ENVIRONMENT': 'develop'
            ])
    )
    Pipeline prodDeployPipeline = new Pipeline(
            name: "${microserviceRepository}-deploy-to-prod",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : microserviceRepository,
                    'ENVIRONMENT': 'prod'
            ])
    )
//    createPipeline(folder, pipeline);
    createPipeline(folder, developDeployPipeline);
    createPipeline(folder, prodDeployPipeline);
}

String androidFolder = createMultibranch('zevrant-android-app', ApplicationType.ANDROID)
Pipeline androidPipeline = new Pipeline(
        name: "zevrant-android-app",
        parameters: new ArrayList<>([
                DefaultPipelineParameters.BRANCH_PARAMETER.getParameter(),
                new PipelineParameter(
                        Boolean.class,
                        "RUN_TESTS",
                        "Whether tests should be ran",
                        Boolean.TRUE
                )
        ]),
        gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
        jenkinsfileLocation: 'jenkins/pipelines/androidBuild.groovy',
        credentialId: 'jenkins-git',
        envs: new HashMap<>([
                'REPOSITORY': 'zevrant-android-app'
        ])
);
createPipeline(androidFolder, androidPipeline)

String createMultibranch(String repositoryName, ApplicationType applicationType) {
    String jobName = ""
    folder(applicationType.value) {

    }
    String folderName = applicationType.value + "/"
    repositoryName.split("-").each { name -> jobName += name.capitalize() + " " }
    jobName = jobName.trim()
    folderName += jobName + "/"
    folder(folderName.substring(0, folderName.length() - 1)) {

    }

    multibranchPipelineJob(folderName + repositoryName + "-multibranch") {
        displayName jobName + " Multibranch"
        factory {
            remoteJenkinsFileWorkflowBranchProjectFactory {
                localMarker("")
                matchBranches(true)
                remoteJenkinsFile("jenkins/pipelines/spring-build.groovy")
                remoteJenkinsFileSCM {
                    gitSCM {
                        extensions {
                            wipeWorkspace()
                            cloneOption {
                                shallow(true)
                                depth(1)
                            }
                        }
                        userRemoteConfigs {
                            userRemoteConfig {
                                name("Zevrant Services Pipeline") //Custom Repository Name or ID
                                url("git@github.com:zevrant/zevrant-services-pipeline.git") //URL for the repository
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
            github {
                id(repositoryName) // IMPORTANT: use a constant and unique identifier
                repository(repositoryName)
                repoOwner('zevrant')
                includes('master develop')
                scanCredentialsId 'jenkins-git-access-token'
                checkoutCredentialsId 'jenkins-git'
                buildOriginBranchWithPR true
                buildOriginPRMerge true
            }

        }
    }
    return folderName;
}

(PipelineCollection.pipelines as List<Pipeline>).each { pipeline ->
    createPipeline("", pipeline)
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
//                if (trigger.type == PipelineTriggerType.GENERIC) {
//                    genericTrigger {
//                        genericVariables {
//                            if (trigger.variables != null && !trigger.variables.isEmpty()) {
//                                trigger.variables.each { variable ->
//                                    genericVariable {
//                                        key(variable.key)
//                                        value(variable.expressionValue)
//                                        expressionType(variable.triggerVariableType.value)
//                                        defaultValue(variable.defaultValue) //Optional, defaults to empty string
//                                    }
//                                }
//                            }
//                        }
//                        token(trigger.token)
//                    }
//                }
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
        if (pipeline.triggers.size() > 0) {
            triggers {
                pipeline.triggers.each { trigger ->
                    switch (trigger.type) {
                        case PipelineTriggerType.CRON:
                            cron(trigger.value);
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

        definition {
            cpsScm {
                lightweight(true)
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