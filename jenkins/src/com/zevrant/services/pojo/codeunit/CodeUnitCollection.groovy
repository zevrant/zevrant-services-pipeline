package com.zevrant.services.pojo.codeunit

abstract class CodeUnitCollection<T extends CodeUnit> {

    protected static List<T> codeUnits;

    static T findByRepoName(String repoName) {
        return codeUnits.find({ codeUnit -> codeUnit.getRepo().getRepoName() == repoName })
    }

    static T findServiceByServiceName(String serviceName) {
        return codeUnits.find({ codeUnit -> codeUnit.name == serviceName })
    }

    static List<T> getCodeUnits() {
        return codeUnits
    }
}
