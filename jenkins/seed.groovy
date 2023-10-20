import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.enumerations.PipelineTrigger
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
    jobDslService.createMultibranch((CodeUnit) libraryCodeUnit)
}

SpringCodeUnitCollection.microservices.each { springCodeUnit ->
    String folder = jobDslService.createMultibranch(springCodeUnit as CodeUnit)
    Pipeline containerBuild = new Pipeline([
            name               : 'container-build',
            description        : "Build containers for ${springCodeUnit.name}",
            jenkinsfileLocation: 'pipelines/applications/spring/container-build.groovy',
            envs               : [repository: springCodeUnit.repo.repoName],
            triggers           : [
                    new PipelineTrigger([
                            type : PipelineTriggerType.UPSTREAM,
                            value: "./${springCodeUnit.name}-multibranch/main"
                    ]),
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
            jenkinsfileLocation: 'jenkins/pipelines/kubernetes-deploy.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'REPOSITORY' : springCodeUnit.name,
                    'ENVIRONMENT': 'develop'
            ]),
            triggers           : [
                    new PipelineTrigger([
                            type : PipelineTriggerType.UPSTREAM,
                            value: "./container-build"
                    ])
            ]
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
    jobDslService.createPipeline(folder,containerBuild)
    jobDslService.createPipeline(folder, developDeployPipeline);
    jobDslService.createPipeline(folder, prodDeployPipeline);
}

AndroidCodeUnitCollection.androidApps.each( { androidCodeUnit ->
    String androidFolder = jobDslService.createMultibranch(androidCodeUnit as CodeUnit)

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
    jobDslService.createPipeline(androidFolder, androidDevelopDeployPipeline)
    jobDslService.createPipeline(androidFolder, androidProdDeployPipeline)
})

String adminFolder = jobDslService.createMultibranch(new CodeUnit([
        name: 'jenkins-cac',
        applicationType: ApplicationType.JENKINS_CAC
]))

(PipelineCollection.pipelines as List<Pipeline>).each { pipeline ->
    jobDslService.createPipeline(pipeline.folder, pipeline)
}


String kubernetesServicesFolder = 'kubernetes-services'
folder(kubernetesServicesFolder) {
    displayName = 'Kubernetes Services'
}

KubernetesServiceCollection.services.each { kubernetesService ->
    kubernetesService.environments.each { environment ->
        Pipeline pipeline = new Pipeline([
                name: kubernetesService.name,
                envs               : [

                        ENVIRONMENT : environment.getNamespaceName(),
                        SERVICE_NAME: kubernetesService.serviceName
                ],
                jenkinsfileLocation: 'jenkins/pipelines/serviceDeploy.groovy'
        ])
        jobDslService.createPipeline(kubernetesServicesFolder.concat('/'), pipeline)
    }
}