package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.ServiceType

class SpringCodeUnit extends CodeUnit {

    private final boolean prodReady
    private final boolean postgresDatabase
    private final String databaseName
    private final String databaseUser
    private final String group
    private final String deploymentName
    private final ServiceType serviceType
    private final boolean enabled

    SpringCodeUnit(Map<String, Object> params) {
        super(params)
        applicationType = ApplicationType.SPRING
        prodReady = params.prodReady ?: false
        postgresDatabase = params.postgresDatabase ?: false
        databaseName = params.databaseName
        databaseUser = params.databaseUser ?: 'zevrant'
        group = params.group ?: 'com.zevrant.services'
        deploymentName = params.deploymentName ?: name
        enabled = (params.containsKey('enabled'))? params.enabled as boolean : true
        serviceType = params.serviceType ?: ServiceType.DEPLOYMENT
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

    String getDatabaseUser() {
        return databaseUser
    }

    String getGroup() {
        return group
    }

    String getDeploymentName() {
        return deploymentName
    }

    boolean getEnabled() {
        return enabled
    }

    ServiceType getServiceType() {
        return serviceType
    }
}
