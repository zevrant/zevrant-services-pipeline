package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class SpringCodeUnit extends CodeUnit {

    private final boolean prodReady
    private final boolean postgresDatabase
    private final String databaseName

    SpringCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.SPRING
        prodReady = params.prodReady ?: false
        postgresDatabase = params.postgresDatabase ?: false
        databaseName = params.databaseName
    }

    boolean getPostgresDatabase() {
        return postgresDatabase
    }

    boolean getProdReady() {
        return prodReady
    }

    String getDatabaseName() {
        return databaseName
    }
}
