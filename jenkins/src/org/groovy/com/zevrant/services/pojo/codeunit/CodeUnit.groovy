package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.GitRepo

class CodeUnit {

    final String name
    final GitRepo repo
    ApplicationType applicationType

    public CodeUnit(Map<String, Object> params) {
        name = params.name as String
        repo = new GitRepo(name)
        applicationType = (ApplicationType) params.applicationType
    }
}
