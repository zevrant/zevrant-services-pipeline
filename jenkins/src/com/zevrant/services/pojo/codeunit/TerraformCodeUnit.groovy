package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType

class TerraformCodeUnit extends CodeUnit {

    private final boolean bareMetal
    private final Map<String, Map<String, String>> envs

    TerraformCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.TERRAFORM
        bareMetal = (params.bareMetal != null) ? params.bareMetal : false
        envs = (params.envs != null) ? params.envs : [:]
    }

    boolean getBareMetal() {
        return bareMetal
    }

    Map<String, Object> getConfigForEnv(String environmentName) {
        return envs.get(environmentName)
    }
}
