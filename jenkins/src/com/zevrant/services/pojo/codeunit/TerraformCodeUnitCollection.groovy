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
                            ],
                            "shared-common": [
                                    proxmox             : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR          : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN         : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    "ingress_virtual_ip": "10.1.0.254/24",
                                    "pki_role"          : [ //TODO use terraform remote state instead of hardcoding
                                                            "backend"   : "pki_shared",
                                                            "issuer_ref": "7149394f-5f66-0579-fced-27db21503f89",
                                                            "name"      : "zevrant-services-shared"
                                    ]

                            ],
                            "shared-blue": [
                                    proxmox       : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR    : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN   : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    load_balancers: [
                                            "01": [
                                                    cpu              : 2,
                                                    default_user     : "zevrant",
                                                    description      : "Keepalived haproxy test",
                                                    gateway          : "10.1.0.1",
                                                    hostname         : "haproxy-shared-blue",
                                                    ip_address       : "10.1.0.5/24",
                                                    is_primary       : true,
                                                    mass_storage_name: "exosDisks",
                                                    memory_mbs       : 4096,
                                                    nameserver       : "10.0.0.8",
                                                    peer_ip_addresses: [
                                                            "10.1.0.12"
                                                    ],
                                                    proxmox_host     : "proxmox-01",
                                                    replica_priority : 255,
                                                    ssd_storage_name : "local-zfs",
                                                    vm_id            : 1020
                                            ]
                                    ]
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