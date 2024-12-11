package com.zevrant.services.pojo

class ProviderShasumsLinks {

    private final String shasumsUpload
    private final String shasumsSigUpload

    ProviderShasumsLinks(String shasumsUpload, String shasumsSigUpload) {
        this.shasumsUpload = shasumsUpload
        this.shasumsSigUpload = shasumsSigUpload
    }

    String getShasumsUpload() {
        return shasumsUpload
    }

    String getShasumsSigUpload() {
        return shasumsSigUpload
    }
}
