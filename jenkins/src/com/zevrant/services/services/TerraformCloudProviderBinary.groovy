package com.zevrant.services.services

class TerraformCloudProviderBinary {
    private String fileName
    private String os
    private String arch
    private String shasum

    TerraformCloudProviderBinary() {
    }

    TerraformCloudProviderBinary(String fileName, String os, String arch, String shasum) {
        this.fileName = fileName
        this.os = os
        this.arch = arch
        this.shasum = shasum
    }

    String getFileName() {
        return fileName
    }

    void setFileName(String fileName) {
        this.fileName = fileName
    }

    String getOs() {
        return os
    }

    void setOs(String os) {
        this.os = os
    }

    String getArch() {
        return arch
    }

    void setArch(String arch) {
        this.arch = arch
    }

    String getShasum() {
        return shasum
    }

    void setShasum(String shasum) {
        this.shasum = shasum
    }
}
