PipelineCollection.pipelines.each { pipeline ->

    pipelineJob(pipeline.name) {
        description pipeline.description
        String jobDisplayName = ""
        pipeline.name.split("-").each {piece ->
            jobDisplayName += piece.capitalize() + " "
        }

        displayName(jobDisplayName.trim())

        logRotator {
            numToKeep 20
        }

        if(pipeline.parameters != null && pipeline.parameters.length > 0) {
            parameters() {
                pipeline.parameters.each { parameter ->
                    switch (parameter.type) {
                        case String.class:
                            stringParam(parameter.name, parameter.defaultValue, parameter.description)
                            break;
                        case Boolean.class:

                        case List.class :

                        default:
                            throw RuntimeException("Parameter not supported")
                    }
                }
            }
        }

        definition {
            cpsScmFlowDefinition {
                scm {
                    gitScm {
                        userRemoteConfigs {
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

                    scriptPath(pipeline.jenkisfileLocation)
                    lightweight(true)
                }
            }
        }
    }
}