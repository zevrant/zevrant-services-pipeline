package com.zevrant.services.pojo.codeunit


import com.zevrant.services.enumerations.SecretType
import com.zevrant.services.pojo.GitRepo
import com.zevrant.services.pojo.SecretMapping

class TerraformCodeUnitCollection extends CodeUnitCollection<TerraformCodeUnit> {

    static {
        codeUnits = [
                new TerraformCodeUnit([
                        name        : 'shared',
                        repo        : new GitRepo(
                                'github.com', 'git@github.com',
                                'zevrant', 'zevrant-services-terraform', 'jenkins-git'
                        ),
                        testsEnabled: false,
                        envs        : [
                                "shared": [
                                        new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins')
                                ]
                        ]
                ])
        ]
    }
}