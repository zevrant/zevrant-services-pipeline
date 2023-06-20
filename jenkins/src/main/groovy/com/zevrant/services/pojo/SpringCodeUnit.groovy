package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class SpringCodeUnit extends CodeUnit {

    private final boolean prodReady
    private final boolean postgresDatabase

    SpringCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.SPRING
        prodReady = params.prodReady ?: false
        postgresDatabase = params.postgresDatabase ?: false
    }

    boolean getPostgresDatabase() {
        return postgresDatabase
    }

    boolean getProdReady() {
        return prodReady
    }
}
