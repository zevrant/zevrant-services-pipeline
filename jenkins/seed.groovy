PipelineCollection.pipelines.each { pipeline ->

    pipelineJob(pipeline.name) {
        description pipeline.description
        String jobDisplayName = ""
        pipeline.name.split("-").each { piece ->
            jobDisplayName += piece.capitalize() + " "
        }

        displayName(jobDisplayName.trim())

        logRotator {
            numToKeep 20
        }

        if (pipeline.parameters != null && pipeline.parameters.size() > 0) {
            parameters {
                nonStoredPasswordParam("APPROVAL_PASSWORD", "test")
                pipeline.parameters.each { parameter ->
                    switch (parameter.type) {
                        case String.class:
                            stringParam(parameter.name, parameter.defaultValue, parameter.description)
                            break;
                        case Boolean.class:

                        case List.class:

                        default:
                            throw RuntimeException("Parameter not supported")
                    }
                }
            }
        }

        definition {
            cpsScmFlowDefinition {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                name('origin')
                                url(pipeline.gitRepo)
                                credentialsId(pipeline.credentialId)
                                refspec('+refs/heads/master:refs/remotes/origin/master')
                            }
                        }

                        branches {
                            branchSpec {
                                name('master')
                            }
                        }

                        browser {
                            gitWeb {
                                repoUrl("https://github.com:zevrant/zevrant-services-pipeline")
                            }
                        }
                        gitTool('')

                        scriptPath(pipeline.jenkinsfileLocation)
                        lightweight(true)
                    }
                }
            }
        }
    }
}