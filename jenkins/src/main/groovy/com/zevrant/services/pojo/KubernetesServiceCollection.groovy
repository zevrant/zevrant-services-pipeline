package com.zevrant.services.pojo

class KubernetesServiceCollection {

    private static final List<KubernetesService> services = Collections.unmodifiableList([
            new KubernetesService([
                    serviceName: 'vault',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.PREPROD_SHARED
                    ],
            ]),
            new KubernetesService([
                    serviceName: 'keycloak',
                    includesDb: true,
                    environments: [
                            KubernetesEnvironment.PREPROD_SHARED
                    ]
            ]),
            new KubernetesService([
                    serviceName: 'gitea',
                    includesDb: true,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'gitea.zevrant-services.internal'
            ]),
            new KubernetesService([
                    serviceName: 'harbor-trivy',
                    includesDb: true,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'docker.io',
                    serviceType: ServiceType.STATEFULSET
            ]),
            new KubernetesService([
                    serviceName: 'harbor-core',
                    includesDb: true,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'harbor.zevrant-services.internal'
            ]),
            new KubernetesService([
                    serviceName: 'prometheus',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.MONITORING
                    ],
                    url: 'prometheus-monitoring.preprod.zevrant-services.internal'
            ]),
    ])

    static List<KubernetesService> getServices() {
        return services
    }

    static KubernetesService findServiceByName(String name) {
        return services.find({ service -> service.serviceName == name})
    }
}
