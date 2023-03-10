package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class AndroidCodeUnit extends CodeUnit{

    AndroidCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.ANDROID
    }
}
