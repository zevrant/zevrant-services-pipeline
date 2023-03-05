package com.zevrant.services.pojo

enum KubernetesEnvironment {
    SHARED,
    PREPROD_SHARED,
    DEVELOP;

    private KubernetesEnvironment() {

    }

    String getNamespaceName() {
        return this.name().toLowerCase().replace('_', '-')
    }
}
