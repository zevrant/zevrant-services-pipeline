package com.zevrant.services.pojo

class KubernetesServiceCollection {

    private static final List<KubernetesService> services = Collections.unmodifiableList([
            new KubernetesService([
                    serviceName: 'vault',
                    url: 'vault.preprod.zevrant-services.internal',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.PREPROD_SHARED
                    ],
            ]),
            new KubernetesService([
                    serviceName: 'keycloak',
                    url: 'keycloak.preprod.zevrant-services.internal',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.PREPROD_SHARED
                    ]
            ]),
            new KubernetesService([
                    serviceName: 'gitea',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'gitea.zevrant-services.internal'
            ]),
            new KubernetesService([
                    serviceName: 'harbor-trivy',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.SHARED
                    ],
                    url: 'docker.io',
                    serviceType: ServiceType.STATEFULSET
            ]),
            new KubernetesService([
                    serviceName: 'harbor-core',
                    includesDb: false,
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
            new KubernetesService([
                    serviceName: 'grafana',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.MONITORING
                    ],
                    url: 'grafana.preprod.zevrant-services.internal'
            ]),
            new KubernetesService([
                    name: 'minio-monitoring',
                    serviceName: 'monitoring-pool-0',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.MONITORING
                    ],
                    url: 'minio-monitoring.preprod.zevrant-services.internal',
                    serviceType: ServiceType.STATEFULSET
            ]),
            new KubernetesService([
                    name: 'jenkins',
                    serviceName: 'jenkins',
                    includesDb: false,
                    environments: [
                            KubernetesEnvironment.JENKINS
                    ],
                    url: 'jenkins.zevrant-services.internal',
                    serviceType: ServiceType.DEPLOYMENT

            ])
    ])

    static List<KubernetesService> getServices() {
        return services
    }

    static KubernetesService findServiceByName(String name) {
        return services.find({ service -> service.name == name })
    }

    static KubernetesService findServiceByServiceName(String serviceName) {
        return services.find({ service -> service.serviceName == serviceName })
    }
}
