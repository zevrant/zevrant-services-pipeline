package com.zevrant.services.pojo.codeunit

import com.zevrant.services.pojo.GitRepo

class PackerCodeUnitCollection {

    public static final List<PackerCodeUnit> packerImages = Collections.unmodifiableList([
        new PackerCodeUnit(
                name: 'alma-base-image',
                extraArguments: [
                        "nodeExporterVersion": "1.8.2"
                ],
                folderPath: 'base-vm',
                repo: new GitRepo(
                        'github.com',  'git@github.com',
                        'zevrant', 'zevrant-services-pipeline',  'jenkins-git'
                )
        )
    ])

    static PackerCodeUnit findCodeUnitByName(String name) {
        List<PackerCodeUnit> codeUnitCollection = packerImages.findAll({ codeUnit -> (codeUnit.name == name) }).asList()
        if (codeUnitCollection.isEmpty()) {
            throw new RuntimeException("Packer Code Unit ${name} not found")
        } else if (codeUnitCollection.size() > 1) {
            throw new RuntimeException("Multiple packer code units were found with the name ${name}")
        }
        return codeUnitCollection.get(0)
    }
}
