package com.zevrant.services.pojo.codeunit

class SpringCodeUnitCollection {

    static final List<SpringCodeUnit> microservices = Collections.unmodifiableList([
            new SpringCodeUnit([
                    name: 'backup-service',
                    postgresDatabase: true,
                    databaseName: 'backup',
            ]),
            new SpringCodeUnit([
                    name: 'acra-backend'
            ]),
            new SpringCodeUnit([
                    name: 'ui-core-ui-core'
            ]),
            new SpringCodeUnit([
                    name: 'oauth2-service'
            ]),
            new SpringCodeUnit([
                    name: 'notification-service'
            ])
    ])

    static SpringCodeUnit findByRepoName(String repoName) {
        return microservices.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

    static SpringCodeUnit findServiceByServiceName(String serviceName) {
        return microservices.find({codeUnit -> codeUnit.name == serviceName})
    }

}
