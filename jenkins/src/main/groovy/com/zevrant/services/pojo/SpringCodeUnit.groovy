package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class SpringCodeUnit extends CodeUnit {

    private final boolean prodReady

    SpringCodeUnit(Map<String, Object> params) {
        super(params)
        prodReady = params.prodReady ?: false
        applicationType = ApplicationType.SPRING
    }
}
