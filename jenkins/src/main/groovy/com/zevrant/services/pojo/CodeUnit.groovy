package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class CodeUnit {

    final String name
    final GitRepo repo
    ApplicationType applicationType

    public CodeUnit(Map<String, Object> params) {
        name = params.name
        repo = new GitRepo(name)
        applicationType = (ApplicationType) params.applicationType
    }
}
