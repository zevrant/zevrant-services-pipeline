package com.zevrant.services.pojo.codeunit

import com.zevrant.services.pojo.GitRepo

class PackerCodeUnit extends CodeUnit {

    private final String folderPath
    private final Map<String, Object> extraArguments
    private final String baseImageName
    private final GitRepo specRepo

    PackerCodeUnit(Map<String, Object> params) {
        super(params)
        folderPath = params.folderPath
        extraArguments = params.extraArguments
        baseImageName = params.baseImageName
        if (params.specRepo == null) {
            params.specRepo = params.repo
        }
        specRepo = params.specRepo

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

    GitRepo getSpecRepo() {
        return specRepo
    }
}
