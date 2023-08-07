package com.zevrant.services.services

import com.zevrant.services.pojo.containers.Image

class ImageBuildService extends Service {

    ImageBuildService(Object pipelineContext) {
        super(pipelineContext)
    }

    List<Image> parseAvailableImages(List<FileWrapper> files) {
        List<Image> images = files.collect({ file ->
            def imageConfig = pipelineContext.readJSON(file: file.path as String)
            def baseImageConfig = imageConfig.baseImage
            Image baseImage = new Image(baseImageConfig.name, baseImageConfig.tag, false, null, baseImageConfig.host, baseImageConfig.repository, null)
            List<String> pathParts = file.path.split('/')
            return new Image(imageConfig.name, imageConfig.version, imageConfig.useLatest, baseImage, "docker.io", "cgdevops", pathParts.subList(0, pathParts.size() -1).join('/'), imageConfig.args)
        })
        return images
    }

    void registryLogin(String username, String token, String registry = 'docker.io') {
        pipelineContext.withEnv(['DOCKER_TOKEN=' + token]) {
            pipelineContext.sh 'echo $DOCKER_TOKEN | buildah login -u ' + username + ' --password-stdin ' + registry //this is very deliberate and intended to prevent secret spillage due to groovy string interpolation DO NOT ALTER WITHOUT KNOWING THE IMPLICATIONS AND DISCUSSING WITH THE TEAM
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
        pipelineContext.sh "buildah push ${image.toString()}"
        if(image.useLatest) {
            String newTag = image.toString().replace(image.version, 'latest')
            pipelineContext.sh "buildah tag ${image.toString()} $newTag"
            pipelineContext.sh "buildah push $newTag"
        }
    }

    void buildImagesInParallel(List<Image> images) {
        if ( images.size() == 0) {
            return
        }
        List<Image> remainingBuilds = []
        def imageBuilds = [:]
        images.each { image ->
            pipelineContext.println("Checking image ${image.baseImage.toString()}")
            boolean existsLocally = doesImageExistLocally(image.baseImage)
            boolean isInQueue = isImageInBuildQueue(image.baseImage, images)
            if (!existsLocally && !isInQueue && !pullBaseImage(image.baseImage)) {
                throw new RuntimeException("Unable to find base image ${image.baseImage.toString()} for image build ${image.toString()}")
            } else if (!existsLocally && isInQueue){
                remainingBuilds.add(image)
            } else {
                imageBuilds[image.toString()] = {
                    buildImage(image)
                    pushImage(image)
                }
            }
        }
        pipelineContext.parallel imageBuilds
        buildImagesInParallel(remainingBuilds)
    }

    boolean pullBaseImage(Image image) {
        return 0 == pipelineContext.sh(returnStatus: true, script: "buildah pull ${image.toString()}")
    }

    boolean doesImageExistLocally(Image image) {
        int status = pipelineContext.sh(returnStatus: true, script: "'buildah images --format \'{{.Name}} {{.Tag}}\' " +
                "| grep ${image.host} " +
                "| grep ${image.repository} " +
                "| grep ${image.name} " +
                "| grep ${image.version}'")
        return status == 0
    }

    static boolean isImageInBuildQueue(Image image, List<Image> images) {
        return images.find {foundImage ->
            return (image.host == foundImage.host
                    && image.repository == foundImage.repository
                    && image.name == foundImage.name
                    && (image.version == image.version || image.version == "latest"))
        }
    }
}
