package com.zevrant.services.pojo.codeunit

class SpringCodeUnitCollection {

    static final List<SpringCodeUnit> microservices = Collections.unmodifiableList([
            new SpringCodeUnit([
                    name: 'backup-service',
                    postgresDatabase: true,
                    databaseName: 'backup',
                    enabled: false
            ]),
            new SpringCodeUnit([
                    name: 'acra-backend',
                    enabled: false
            ]),
            new SpringCodeUnit([
                    name: 'ui-core',
                    enabled: false
            ]),
            new SpringCodeUnit([
                    name: 'oauth2-service',
                    enabled: true
            ]),
            new SpringCodeUnit([
                    name: 'notification-service',
                    enabled: false
            ])
    ])

    static SpringCodeUnit findByRepoName(String repoName) {
        return microservices.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

    static SpringCodeUnit findServiceByServiceName(String serviceName) {
        return microservices.find({codeUnit -> codeUnit.name == serviceName})
    }

}
