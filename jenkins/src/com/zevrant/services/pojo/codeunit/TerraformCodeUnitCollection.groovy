package com.zevrant.services.pojo.codeunit


import com.zevrant.services.enumerations.SecretType
import com.zevrant.services.pojo.GitRepo
import com.zevrant.services.pojo.SecretMapping

class TerraformCodeUnitCollection {

    private static List<TerraformCodeUnit> codeUnits = [
            new TerraformCodeUnit([
                    name        : 'shared',
                    repo        : new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-terraform', 'jenkins-git'
                    ),
                    testsEnabled: false,
                    envs        : [
                            "shared": [
                                    proxmox: new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN: new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    hcp_client      : new SecretMapping(SecretType.HCP_CLIENT, ''),
                                    CF_DNS_API_TOKEN: new SecretMapping(SecretType.SECRET_TEXT, 'shared/cloudflare-dns-api-token', true)
                            ]
                    ]
            ]),
            new TerraformCodeUnit([
                    name        : 's3-garage',
                    repo        : new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 's3-garage', 'jenkins-git'
                    ),
                    testsEnabled: false,
                    envs        : [
                            secrets    : [
                                    proxmox    : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN: new SecretMapping(SecretType.VAULT_TOKEN, ''),
                            ],
                            application: [
                                    proxmox    : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN: new SecretMapping(SecretType.VAULT_TOKEN, '')
                            ],
                    ]
            ])
    ]

    static TerraformCodeUnit findByRepoName(String repoName) {
        return codeUnits.find({ codeUnit -> codeUnit.getRepo().getRepoName() == repoName })
    }

    static TerraformCodeUnit findServiceByServiceName(String serviceName) {
        return codeUnits.find({ codeUnit -> codeUnit.name == serviceName })
    }

    static List<TerraformCodeUnit> getCodeUnits() {
        return codeUnits
    }
}