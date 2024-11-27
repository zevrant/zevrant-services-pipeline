import com.zevrant.services.enumerations.PipelineTriggerType
import com.zevrant.services.pojo.Pipeline
import com.zevrant.services.pojo.PipelineTrigger
import com.zevrant.services.pojo.codeunit.PackerCodeUnit
import com.zevrant.services.services.JobDslService
import org.apache.commons.lang.StringUtils

@Library('CommonUtils')

JobDslService jobDslService = new JobDslService(this)


(images as List<PackerCodeUnit>).each { image ->
    List<PipelineTrigger> pipelineTriggers = []
    if (StringUtils.isNotBlank(image.baseImageName)) {
        pipelineTriggers.add(new PipelineTrigger([
                type : PipelineTriggerType.UPSTREAM,
                value: "/packer/build-${image.repo.repoName.split('/').collect({ it.toLowerCase() }).join('-')}"
        ]))
    }


    jobDslService.createPipeline('/packer/', new Pipeline([
            name               : "build-${image.repo.repoName.split('/').collect({ it.toLowerCase() }).join('-')}",
            gitRepo            : image.repo.getSshHostName(),
            jenkinsfileLocation: 'jenkins/pipelines/packer/packer-build.groovy',
            credentialId       : 'jenkins-git',
            envs               : new HashMap<>([
                    'BUILD_DIR_PATH': image.getFolderPath(),
                    'NAME': image.name
            ]),
            triggers           : pipelineTriggers
    ]))
}
