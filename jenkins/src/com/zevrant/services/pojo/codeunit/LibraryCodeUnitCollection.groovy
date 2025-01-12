package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType

class LibraryCodeUnitCollection  {

    final static libraries = Collections.unmodifiableList([
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
