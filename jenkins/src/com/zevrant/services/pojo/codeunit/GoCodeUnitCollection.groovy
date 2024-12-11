package com.zevrant.services.pojo.codeunit

class GoCodeUnitCollection {

    private static final List<GoCodeUnit> codeUnits = Collections.unmodifiableList([

    ])

    static SpringCodeUnit findCodeUnitByRepositoryName(String repoName) {
        return codeUnits.find({codeUnit -> codeUnit.getRepo().getRepoName() == repoName})
    }

}
