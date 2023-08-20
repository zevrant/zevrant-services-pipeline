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
                    serviceName: 'artifactory-jcr',
                    includesDb: true,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'docker.io',
                    serviceType: ServiceType.STATEFULSET
            ])
    ])

    static List<KubernetesService> getServices() {
        return services
    }

    static KubernetesService findServiceByName(String name) {
        return services.find({ service -> service.serviceName == name})
    }
}
