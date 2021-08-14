import com.zevrant.services.DefaultPipelineParameters
import com.zevrant.services.Pipeline
import com.zevrant.services.PipelineCollection
import com.zevrant.services.PipelineTriggerType

(libraryRepositories as List<String>).each { libraryRepository ->

    String jobName = ""
    folder("Libraries") {
        libraryRepository.split("-").each { repositoryName -> jobName += repositoryName.capitalize() + " " }
        jobName = jobName.trim()
        folder(jobName) {
            multibranchPipelineJob(libraryRepository + "-multibranch") {
                displayName jobName += " Multibranch"
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
            createPipeline(pipeline)
        }
    }
}
(PipelineCollection.pipelines as List<Pipeline>).each { pipeline ->
    createPipeline(pipeline)
}

void createPipeline(Pipeline pipeline) {
    pipelineJob(pipeline.name) {
        description pipeline.description
        String jobDisplayName = ""
        pipeline.name.split("-").each { piece ->
            jobDisplayName += piece.capitalize() + " "
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
                        default:
                            throw new RuntimeException("Pipeline Trigger Type Not Implemented ${trigger.Type} for pipeline ${pipeline.name}")
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