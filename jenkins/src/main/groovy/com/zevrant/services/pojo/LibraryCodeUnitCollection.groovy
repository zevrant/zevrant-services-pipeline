package com.zevrant.services.pojo

class LibraryCodeUnitCollection  {

    final static javaLibraries = Collections.unmodifiableList([
            new LibraryCodeUnit(
                name: 'universal-common'
            ),
            new LibraryCodeUnit([
                    name: 'security-common'
            ])
    ])
}
