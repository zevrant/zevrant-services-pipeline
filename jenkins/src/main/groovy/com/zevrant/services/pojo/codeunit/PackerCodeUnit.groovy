package com.zevrant.services.pojo.codeunit

class PackerCodeUnit extends CodeUnit {

    private final String folderPath;
    private final Map<String, Object> extraArguments;
    private final String baseImageName;

    PackerCodeUnit(Map<String, Object> params) {
        super(params)
        folderPath = params.folderPath
        extraArguments = params.extraArguments
        baseImageName = params.baseImageName
    }

    String getFolderPath() {
        return folderPath
    }

    Map<String, Object> getExtraArguments() {
        return extraArguments
    }

    String getBaseImageName() {
        return baseImageName
    }
}
