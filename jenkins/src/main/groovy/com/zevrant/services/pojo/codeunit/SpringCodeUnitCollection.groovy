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
                    name: 'ui-core'
            ]),
            new SpringCodeUnit([
                    name: 'oauth2-service'
            ]),
            new SpringCodeUnit([
                    name: 'notification-service'
            ])
    ])


}
