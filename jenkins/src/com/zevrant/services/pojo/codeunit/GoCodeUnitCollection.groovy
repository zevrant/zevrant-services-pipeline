package com.zevrant.services.pojo.codeunit

class GoCodeUnitCollection {

    private static final List<GoCodeUnit> codeUnits = Collections.unmodifiableList([
        new GoCodeUnit([
                name: 'terraform-provider-proxmox'
        ])
    ])

    static SpringCodeUnit findCodeUnitByRepositoryName(String repoName) {
        return codeUnits.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

}
