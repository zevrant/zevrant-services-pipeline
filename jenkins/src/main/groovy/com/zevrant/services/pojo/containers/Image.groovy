package com.zevrant.services.pojo.containers

class Image {
    private String name
    private String version
    private boolean useLatest
    private Image baseImage
    private String host
    private String repository
    private String buildDirPath
    private List<String> args
    Image(String name, String version, boolean useLatest, Image baseImage, String host, String repository, String buildDirPath, List<String> args = []) {
        this.name = name
        this.version = version
        this.useLatest = useLatest
        this.baseImage = baseImage
        this.host = host
        this.repository = repository
        this.buildDirPath = buildDirPath
        this.args = args
    }

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
    }

    boolean getUseLatest() {
        return useLatest
    }

    void setUseLatest(boolean useLatest) {
        this.useLatest = useLatest
    }

    Image getBaseImage() {
        return baseImage
    }

    void setBaseImage(Image baseImage) {
        this.baseImage = baseImage
    }

    String getHost() {
        return host
    }

    void setHost(String host) {
        this.host = host
    }

    String getRepository() {
        return repository
    }

    void setRepository(String repository) {
        this.repository = repository
    }

    String getBuildDirPath() {
        return buildDirPath
    }

    void setBuildDirPath(String buildDirPath) {
        this.buildDirPath = buildDirPath
    }

    String toString() {
        return "${host}/${repository}/${name}:${version}".replace('//', '/')
    }

    List<String> getArgs() {
        return args
    }

    void setArgs(List<String> args) {
        this.args = args
    }
}