package com.zevrant.services.pojo

enum KubernetesEnvironment {
    SHARED,
    PREPROD_SHARED,
    DEVELOP,
    MONITORING;

    private KubernetesEnvironment() {

    }

    String getNamespaceName() {
        return this.name().toLowerCase().replace('_', '-')
    }
}
