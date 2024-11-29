package com.zevrant.services.pojo.codeunit

import com.zevrant.services.pojo.GitRepo

class PackerCodeUnitCollection {

    public static final List<PackerCodeUnit> packerImages = Collections.unmodifiableList([
        new PackerCodeUnit(
                name: 'base-vm',
                extraArguments: [
                        "nodeExporterVersion": "1.8.2"
                ],
                folderPath: 'base-vm',
                repo: new GitRepo(
                        'github.com',  'git@github.com',
                        'zevrant', 'packer-build-specs',  'jenkins-git'
                )
        )
    ])

    static PackerCodeUnit findCodeUnitByName(String name) {
        Optional<PackerCodeUnit> codeUnitOptional = packerImages.stream().filter { codeUnit -> (codeUnit.name == name) }.findFirst()
        if (codeUnitOptional.isEmpty()) {
            throw new RuntimeException("Packer Code Unit ${name} not found")
        }
        return codeUnitOptional.get()
    }
}
