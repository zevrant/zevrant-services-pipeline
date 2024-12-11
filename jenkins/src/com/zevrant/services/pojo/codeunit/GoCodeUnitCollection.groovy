package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.GitRepo

class GoCodeUnitCollection {

    private static final List<GoCodeUnit> codeUnits = Collections.unmodifiableList([
        new GoCodeUnit([
                name: 'terraform-provider-proxmox',
                applicationType: ApplicationType.TERRAFORM_PROVIDER,
                repo: new GitRepo(
                        'github.com',  'git@github.com',
                        'zevrant', 'terraform-provider-proxmox',  'jenkins-git'
                )
        ])
    ])

    static SpringCodeUnit findCodeUnitByRepositoryName(String repoName) {
        return codeUnits.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

}
