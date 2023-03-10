package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class LibraryCodeUnit extends CodeUnit {

    LibraryCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.JAVA_LIBRARY
    }
}
