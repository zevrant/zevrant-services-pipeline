import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.services.JobDslService

JobDslService jobDslService = new JobDslService(this)

(images as List<String>).each {imageString ->
    //<jenkins>harbor.zevrant-services.internal/dockerhub/gitea/gitea:latest-rootless
    List<String> imageInfo = imageString.split('/')
    String imageName = imageInfo.get(imageInfo.size() - 1).split(':')[0]
    String repository = (imageInfo.size() == 4)? imageInfo.get(1) + '/' + imageInfo.get(2) : imageInfo.get(1)
    String buildDirPath = "${imageInfo.get(0).split('>')[0].replace('<', '')}/${imageName}"
    jobDslService.createPipeline(FOLDER_PATH as String, new Pipeline([
            name: "build-${repository.split('/').collect({it.capitalize()}).join('-')}-${imageName}",
            gitRepo: 'ssh://git@gitea.zevrant-services.internal:30121/zevrant-services/zevrant-services-pipeline.git',
            jenkinsfileLocation: 'jenkins/pipelines/containers/buildContainerImage.groovy',
            credentialId: 'jenkins-git',
            envs: new HashMap<>([
                    'BUILD_DIR_PATH': buildDirPath
            ]),
    ]))
}
