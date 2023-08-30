import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.PipelineParameter
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.containers.Image
import com.zevrant.services.pojo.PipelineCollection
import com.zevrant.services.pojo.KubernetesServiceCollection
import com.zevrant.services.pojo.codeunit.LibraryCodeUnitCollection
import com.zevrant.services.pojo.codeunit.AndroidCodeUnitCollection
import com.zevrant.services.pojo.codeunit.SpringCodeUnitCollection
import com.zevrant.services.pojo.codeunit.CodeUnit
import com.zevrant.services.services.JobDslService

JobDslService jobDslService = new JobDslService(this)

folder('containers') {
    displayName('Containers')
}
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
            gitRepo: 'ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git',
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : springCodeUnit.name,
                    'ENVIRONMENT': 'develop'
            ]),
    )
    Pipeline prodDeployPipeline = new Pipeline(
            name: "${springCodeUnit}-deploy-to-prod",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: 'ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git',
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : springCodeUnit.name,
                    'ENVIRONMENT': 'prod'
            ]),
    )
    jobDslService.createPipeline(folder, developDeployPipeline);
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
            gitRepo: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git",
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
            gitRepo: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git",
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

String adminFolder = createMultibranch(new CodeUnit([
        name: 'jenkins-cac',
        applicationType: ApplicationType.JENKINS_CAC
]))

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
//            git {
//                id(codeUnit.name) // IMPORTANT: use a constant and unique identifier
//                remote("ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/${codeUnit.name}.git")
//                credentialsId 'jenkins-git-access-token'
//                checkoutCredentialsId 'jenkins-git'
//            }
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
                name: kubernetesService.serviceName,
                envs               : [

                        ENVIRONMENT : environment.getNamespaceName(),
                        SERVICE_NAME: kubernetesService.serviceName
                ],
                jenkinsfileLocation: 'jenkins/pipelines/serviceDeploy.groovy'
        ])
        createPipeline(kubernetesServicesFolder.concat('/'), pipeline)
    }
}