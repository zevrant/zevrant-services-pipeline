package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.GitRepo
import org.apache.commons.lang.StringUtils

class CodeUnit {

    final String name
    final GitRepo repo
    final String defaultBranch
    ApplicationType applicationType

    public CodeUnit(Map<String, Object> params) {
        name = params.name as String
        repo = (params.repo != null)? params.repo as GitRepo :new GitRepo(name)
        defaultBranch = StringUtils.isNotBlank(params.defaultBranch)? params.defaultBranch : "master"
        applicationType = (ApplicationType) params.applicationType
    }

}
