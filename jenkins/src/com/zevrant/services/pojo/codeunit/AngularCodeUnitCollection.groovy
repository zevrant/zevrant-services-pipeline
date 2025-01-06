package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.GitRepo

class AngularCodeUnitCollection {

    private static final List<AngularCodeUnit> codeUnits = Collections.unmodifiableList([
            new AngularCodeUnit([
                    name           : 'the-fence-gurus',
                    applicationType: ApplicationType.ANGULAR,
                    repo           : new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 'the-fence-gurus', 'jenkins-git'
                    )
            ])
    ])

    static AngularCodeUnit findCodeUnitByRepositoryName(String repoName) {
        return codeUnits.find({ codeUnit -> codeUnit.getRepo().getRepoName() == repoName })
    }

}
