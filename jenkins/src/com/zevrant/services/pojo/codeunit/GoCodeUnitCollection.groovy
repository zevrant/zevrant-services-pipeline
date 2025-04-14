package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.ApplicationType
import com.zevrant.services.pojo.GitRepo

class GoCodeUnitCollection {

    private static final List<GoCodeUnit> codeUnits = Collections.unmodifiableList([
        new GoCodeUnit([
                name: 'terraform-provider-proxmox',
                applicationType: ApplicationType.TERRAFORM_PROVIDER,
                providerOrgName: 'zevrant-services',
                repo: new GitRepo(
                        'github.com',  'git@github.com',
                        'zevrant', 'terraform-provider-proxmox',  'jenkins-git'
                )
        ]),
        new GoCodeUnit([
                name           : 'zs-vm-agent',
                applicationType: ApplicationType.GO,
                repo           : new GitRepo(
                        'github.com', 'git@github.com',
                        'zevrant', 'zs-vm-agent', 'jenkins-git'
                ),

        ])
    ])

    static GoCodeUnit findCodeUnitByRepositoryName(String repoName) {
        return codeUnits.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

}
