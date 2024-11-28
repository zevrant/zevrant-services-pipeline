package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType

class LibraryCodeUnit extends CodeUnit {

    LibraryCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = params.applicationType as ApplicationType
    }
}
