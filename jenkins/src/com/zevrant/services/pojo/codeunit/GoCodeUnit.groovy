package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType

class GoCodeUnit extends CodeUnit{

    GoCodeUnit(Map<String, Object> params) {
        super(params)
        if (params.applicationType == null) {
            applicationType = ApplicationType.GO
        }
    }

}