import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.*
import com.zevrant.services.pojo.codeunit.*
import com.zevrant.services.services.JobDslService
import com.zevrant.services.pojo.codeunit.TerraformCodeUnit
import com.zevrant.services.pojo.codeunit.TerraformCodeUnitCollection

JobDslService jobDslService = new JobDslService(this)

folder('/games') {
    displayName('Games')
}

folder('/containers') {
    displayName('Containers')
}

folder('/packer') {
    displayName('Packer')
}

LibraryCodeUnitCollection.libraries.each { libraryCodeUnit ->
    jobDslService.createMultibranch((CodeUnit) libraryCodeUnit)
}

SpringCodeUnitCollection.microservices.each { springCodeUnit ->
    String folder = jobDslService.createMultibranch(springCodeUnit as CodeUnit)
    Pipeline containerBuild = new Pipeline([
            name               : 'container-build',
            description        : "Build containers for ${springCodeUnit.name}",
            jenkinsfileLocation: 'jenkins/pipelines/spring/container-build.groovy',
            envs               : [repository: springCodeUnit.repo.repoName],
            triggers           : [
                    new PipelineTrigger([
                            type : PipelineTriggerType.UPSTREAM,
                            value: "/containers/build-Zevrant-services-ubuntu-base"
                    ])
            ]
    ])
    Pipeline developDeployPipeline = new Pipeline(
            name: "${springCodeUnit.name}-deploy-to-develop",
            parameters: new ArrayList<>([
                    new PipelineParameter<String>(String.class, "VERSION", "Version to be Deployed", "")
            ]),
            gitRepo: 'ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git',
            jenkinsfileLocation: 'jenkins/pipelines/spring/kubernetes-deploy.groovy',
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
    jobDslService.createPipeline(folder, containerBuild)
    jobDslService.createPipeline(folder, developDeployPipeline);
//    jobDslService.createPipeline(folder, prodDeployPipeline);
}

AndroidCodeUnitCollection.androidApps.each({ androidCodeUnit ->
    String androidFolder = jobDslService.createMultibranch(androidCodeUnit as CodeUnit)

    Pipeline androidDevelopDeployPipeline = new Pipeline(
            name: "${androidCodeUnit.name}-Release-To-Internal-Testing",
            parameters: new ArrayList<>([
            ]),
            gitRepo: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/android-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : androidCodeUnit.name,
                    'ENVIRONMENT': 'develop'
            ])
    )
    Pipeline androidProdDeployPipeline = new Pipeline(
            name: "${androidCodeUnit.name}-Release-To-Production",
            parameters: new ArrayList<>([
            ]),
            gitRepo: "ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git",
            jenkinsfileLocation: 'jenkins/pipelines/android-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : androidCodeUnit.name,
                    'ENVIRONMENT': 'prod'
            ])
    )
    jobDslService.createPipeline(androidFolder, androidDevelopDeployPipeline)
    jobDslService.createPipeline(androidFolder, androidProdDeployPipeline)
})

AngularCodeUnitCollection.codeUnits.each({ angularCodeUnit ->
    String angularFolder = jobDslService.createMultibranch(angularCodeUnit as CodeUnit)
})

String adminFolder = jobDslService.createMultibranch(new CodeUnit([
        name           : 'jenkins-cac',
        applicationType: ApplicationType.JENKINS_CAC
]))

(PipelineCollection.pipelines as List<Pipeline>).each { pipeline ->
    jobDslService.createPipeline(pipeline.folder, pipeline)
}


String kubernetesServicesFolder = '/kubernetes-services'
folder(kubernetesServicesFolder) {
    displayName = 'Kubernetes Services'
}

KubernetesServiceCollection.services.each { kubernetesService ->
    kubernetesService.environments.each { environment ->
        Pipeline pipeline = new Pipeline([
                name               : kubernetesService.name,
                envs               : [

                        ENVIRONMENT : environment.getNamespaceName(),
                        SERVICE_NAME: kubernetesService.serviceName
                ],
                jenkinsfileLocation: 'jenkins/pipelines/serviceDeploy.groovy'
        ])
        jobDslService.createPipeline(kubernetesServicesFolder.concat('/'), pipeline)
    }
}

GoCodeUnitCollection.codeUnits.each { codeUnit ->
    String folder = jobDslService.createMultibranch(codeUnit as CodeUnit)
    GoCodeUnit goCodeUnit = codeUnit as GoCodeUnit
    String codeUnitTitle = goCodeUnit.name.split('-').collect({ item -> item.capitalize() }).join(' ')
    if (goCodeUnit.providerOrgName != null && goCodeUnit.providerOrgName != "") {
        Pipeline providerRelease = new Pipeline(
                name: "${goCodeUnit.name}-publish-to-terraform-cloud",
                parameters: new ArrayList<>([]),
                credentialId: 'jenkins-git',
                gitRepo: 'git@github.com:zevrant/zevrant-services-pipeline.git',
                jenkinsfileLocation: 'jenkins/pipelines/terraform/provider-release.groovy',
                envs: new HashMap<>([
                        'REPOSITORY': goCodeUnit.name,
                ]),
                triggers: [
                        new PipelineTrigger([
                                type : PipelineTriggerType.UPSTREAM,
                                value: "./${codeUnitTitle}-multibranch/master"
                        ])
                ]
        )
        jobDslService.createPipeline(folder, providerRelease)
    }
}

TerraformCodeUnitCollection.codeUnits.each { codeUnit ->
    String folder = jobDslService.createMultibranch(codeUnit as CodeUnit)
    TerraformCodeUnit terraformCodeUnit = codeUnit as CodeUnit


    terraformCodeUnit.getEnvs().each { env ->
        Pipeline terraformPipeline = new Pipeline(
                name: "${terraformCodeUnit.name.toLowerCase()}-deploy-to-${env}",
                parameters: new ArrayList<>([]),
                credentialId: 'jenkins-git',
                gitRepo: 'git@github.com:zevrant/zevrant-services-pipeline.git',
                jenkinsfileLocation: 'jenkins/pipelines/terraform/deploy.groovy',
                envs: [
                        REPOSITORY: terraformCodeUnit.repo.repoName,
                        ENVIRONMENT: env
                ],


        )
        if (terraformCodeUnit.getEnvs().get(env).hasKey("trigger")) {
            def envObj = terraformCodeUnit.getEnvs().get(env)
            terraformPipeline.triggers = [
                    new PipelineTrigger(
                            type: envObj.trigger.type,
                            value: envObj.trigger.value
                    )
            ]
        }
        jobDslService.createPipeline(folder, terraformPipeline)
    }
}