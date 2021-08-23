import com.zevrant.services.DefaultPipelineParameters
import com.zevrant.services.Pipeline
import com.zevrant.services.PipelineCollection
import com.zevrant.services.PipelineTriggerType

(libraryRepositories as List<String>).each { libraryRepository ->

    String jobName = ""
    folder("Libraries") {

    }
    String folderName = "Libraries/"
    libraryRepository.split("-").each { repositoryName -> jobName += repositoryName.capitalize() + " " }
    jobName = jobName.trim()
    folderName += jobName + "/"
    folder(folderName.substring(0, folderName.length() -1)) {

    }
    multibranchPipelineJob(folderName + libraryRepository + "-multibranch") {
        displayName jobName + " Multibranch"
        factory {
            workflowBranchProjectFactory {
                scriptPath('Jenkinsfile.groovy')
            }
        }
        branchSources {
            github {
                id(libraryRepository) // IMPORTANT: use a constant and unique identifier
                repository(libraryRepository)
                repoOwner('zevrant')
                includes('master')
                scanCredentialsId 'jenkins-git-access-token'
                checkoutCredentialsId 'jenkins-git'
            }

        }
    }
    Pipeline pipeline = new Pipeline(
            name: libraryRepository,
            parameters: new ArrayList<>([
                    DefaultPipelineParameters.BRANCH_PARAMETER.getParameter()
            ]),
            gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/libraryBuild.groovy',
            credentialId: 'jenkins-git'
    );
    createPipeline(folderName, pipeline)
}

String jobName = "zevrant-android-app"
String displayNameString = ""
folder("Android") {}
String folderName = "Android/"
jobName.split("-").each { repositoryName -> displayNameString += repositoryName.capitalize() + " " }
displayNameString = jobName.trim()
folderName += displayNameString + "/"
folder(folderName.substring(0, folderName.length() -1)) {}
multibranchPipelineJob(folderName + jobName + "-multibranch") {
    displayName displayNameString + "Multibranch"
    factory {
        workflowBranchProjectFactory {
            scriptPath('Jenkinsfile.groovy')
        }
    }
    branchSources {
        github {
            id(jobName) // IMPORTANT: use a constant and unique identifier
            repository('zevrant-android-app')
            repoOwner('zevrant')
            includes('master')
            scanCredentialsId 'jenkins-git-access-token'
            checkoutCredentialsId 'jenkins-git'
        }

    }
}
Pipeline androidPipeline = new Pipeline(
        name: "zevrant-android-app",
        parameters: new ArrayList<>([
                DefaultPipelineParameters.BRANCH_PARAMETER.getParameter()
        ]),
        gitRepo: "git@github.com:zevrant/zevrant-services-pipeline.git",
        jenkinsfileLocation: 'jenkins/pipelines/libraryBuild.groovy',
        credentialId: 'jenkins-git'
);
createPipeline(folderName, androidPipeline)

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

        if(pipeline.triggers != null && !pipeline.triggers.isEmpty()) {
            pipeline.triggers.each { trigger ->
                if (trigger.type == PipelineTriggerType.GENERIC) {
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
                            }
                        }
                    }
                }
                scriptPath(pipeline.jenkinsfileLocation)
            }
        }
    }
}