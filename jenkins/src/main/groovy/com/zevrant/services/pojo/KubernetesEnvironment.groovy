package com.zevrant.services.pojo

enum KubernetesEnvironment {
    SHARED,
    PREPROD_SHARED,
    DEVELOP,
    JENKINS,
    MONITORING,
    PROD;

    private KubernetesEnvironment() {

    }

    String getNamespaceName() {
        return this.name().toLowerCase().replace('_', '-')
    }
}
