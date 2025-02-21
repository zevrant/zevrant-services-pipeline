package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType

class AngularCodeUnit extends CodeUnit {

    private final boolean testsEnabled

    AngularCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.ANGULAR
        testsEnabled = (params.testsEnabled == null) ? true : params.testsEnabled
    }

    boolean getTestsEnabled() {
        return testsEnabled
    }
}
