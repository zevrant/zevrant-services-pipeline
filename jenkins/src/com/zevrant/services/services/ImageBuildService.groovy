package com.zevrant.services.services

import Image
import JSONObject
import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper

class ImageBuildService extends Service {

    ImageBuildService(Object pipelineContext) {
        super(pipelineContext)
    }

    List<Image> parseAvailableImages(List<FileWrapper> files, String registry, String project) {
        List<Image> images = files.collect({ file ->
            def imageConfig = pipelineContext.readJSON(file: file.path as String)
            parseImageConfig(imageConfig, file.path)
        })
        return images.findAll ({ image -> image != null })
    }

    Image parseImageConfig(JSONObject imageConfig, String filePath, String registry = 'harbor.zevrant-services.internal', String project = 'zevrant-services') {
        def baseImageConfig = imageConfig.baseImage
        pipelineContext.println("Parsing image ${filePath}")
        Image baseImage = new Image(baseImageConfig.name, baseImageConfig.tag, false, null, baseImageConfig.host, baseImageConfig.repository, null)
        List<String> pathParts = filePath.split('/')
        return new Image(imageConfig.name, imageConfig.version, imageConfig.useLatest, baseImage, registry, project,
                pathParts.subList(0, pathParts.size() -1).join('/'), imageConfig.args)
    }

    void registryLogin(String username, String token, String registry = 'docker.io') {
        pipelineContext.withEnv(['DOCKER_TOKEN=' + token, 'DOCKER_USERNAME=' + username]) {
            pipelineContext.sh 'echo $DOCKER_TOKEN | buildah login -u "$DOCKER_USERNAME" --password-stdin ' + registry //this is very deliberate and intended to prevent secret spillage due to groovy string interpolation DO NOT ALTER WITHOUT KNOWING THE IMPLICATIONS AND DISCUSSING WITH THE TEAM
        }
    }

    void buildImage(Image image) {
        pipelineContext.dir(image.buildDirPath) {
            String args = ''
            if (image.getArgs() != null && !image.getArgs().isEmpty()) {
                args = "--build-arg ${image.getArgs().join(" --build-arg ")}"
            }
            pipelineContext.sh "buildah bud ${args} -t ${image.toString()}"
        }
    }

    void pushImage(Image image) {
        pipelineContext.retry(3, {
            pipelineContext.sh "buildah push ${image.toString()}"
        })
        if(image.useLatest) {
            String newTag = image.toString().replace(image.version, 'latest')
            pipelineContext.sh "buildah tag ${image.toString()} $newTag"
            pipelineContext.retry(3, {
                pipelineContext.sh "buildah push $newTag"
            })
        }
    }

    List<Image> buildImages(List<Image> images) {
        if ( images.size() == 0) {
            return
        }
        List<Image> remainingBuilds = []
        images.each { image ->
            pipelineContext.println("Checking image ${image.baseImage.toString()}")
            boolean existsLocally = doesImageExistLocally(image.baseImage)
            boolean isInQueue = isImageInBuildQueue(image.baseImage, images)
            if (!existsLocally && !isInQueue && !pullBaseImage(image.baseImage)) {
                throw new RuntimeException("Unable to find base image ${image.baseImage.toString()} for image build ${image.toString()}")
            } else if (isInQueue){
                remainingBuilds.add(image)
            } else {
                boolean exists = doesImageExistLocally(image.baseImage)

                while (!exists) {
                    pipelineContext.println "Waiting for ${image.baseImage.toString()} to exist locally"
                    sleep 15
                    exists = doesImageExistLocally(image.baseImage)
                }
                pipelineContext.build(job: "/containers/build-${image.repository.split('/').collect({ it.capitalize() }).join('-')}-${image.name}", wait: false, waitForStart: true)
            }
        }
        return remainingBuilds
    }

    boolean pullBaseImage(Image image) {
        pipelineContext.retry(3, {
            return 0 == pipelineContext.sh(returnStatus: true, script: "buildah pull ${image.toString()}")
        })
    }

    boolean doesImageExistLocally(Image image) {
        String imageName = "${image.host}/${image.repository}/${image.name}".replace('//', '/')
        int status = pipelineContext.sh(returnStatus: true, script: "buildah images ${image.toString()} | grep ${imageName}")
        pipelineContext.println "Return Status was  ${status}"
        return 0 == status
    }

    boolean isImageInBuildQueue(Image image, List<Image> images) {
        return images.find {foundImage ->
            pipelineContext.println("${image.host} == $foundImage.host (${image.host == foundImage.host})\n" +
                    "                    && $image.repository == $foundImage.repository (${image.repository == foundImage.repository})\n" +
                    "                    && $image.name == $foundImage.name (${image.name == foundImage.name})\n" +
                    "                    && ($image.version == $image.version || $image.version == \"latest\") (${image.version == image.version || image.version == "latest"})")

            return (image.host == foundImage.host
                    && image.repository == foundImage.repository
                    && image.name == foundImage.name
                    && (image.version == image.version || image.version == "latest"))
        }
    }
}
