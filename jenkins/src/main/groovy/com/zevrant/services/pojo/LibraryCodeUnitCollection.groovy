package com.zevrant.services.pojo

import com.zevrant.services.enumerations.ApplicationType

class LibraryCodeUnitCollection  {

    final static kibraries = Collections.unmodifiableList([
            new LibraryCodeUnit(
                name: 'universal-common',
                applicationType: ApplicationType.JAVA_LIBRARY
            ),
            new LibraryCodeUnit([
                    name: 'security-common',
                    applicationType: ApplicationType.JAVA_LIBRARY
            ])
    ])
}
