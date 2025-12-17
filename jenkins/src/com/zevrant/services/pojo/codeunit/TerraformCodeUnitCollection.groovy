package com.zevrant.services.pojo.codeunit

import com.zevrant.services.enumerations.PipelineTriggerType
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
//                            "shared"       : [
//                                    proxmox    : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
//                                    VAULT_ADDR : 'https://vault.zevrant-services.com',
//                                    VAULT_TOKEN: new SecretMapping(SecretType.VAULT_TOKEN, ''),
//                                    hcp_client      : new SecretMapping(SecretType.HCP_CLIENT, ''),
//                                    CF_DNS_API_TOKEN: new SecretMapping(SecretType.SECRET_TEXT, 'shared/cloudflare-dns-api-token', true),
//                                    trigger    : [
//                                            type : PipelineTriggerType.CRON,
//                                            value: "0 0 * * *"
//                                    ],
//                                    bitwarden_access_token: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/bitwarden')
//                            ],
                            "shared-common": [
                                    hcp_client    : new SecretMapping(SecretType.HCP_CLIENT, ''),
                                    proxmox       : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR    : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN   : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    MINIO_USER    : new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-username', true),
                                    MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true),
                                    trigger       : [
                                            type : PipelineTriggerType.CRON,
                                            value: "0 0 * * *"
                                    ],

                            ],
                            "shared-blue"  : [trigger       : [
                                    type : PipelineTriggerType.UPSTREAM,
                                    value: 'shared-deploy-to-shared-green'
                            ],
                                              proxmox       : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                              VAULT_ADDR    : 'https://vault.zevrant-services.com',
                                              VAULT_TOKEN   : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                              load_balancers: [
                                                      "01": [
                                                              cpu              : 2,
                                                              default_user     : "zevrant",
                                                              description      : "Keepalived haproxy",
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
                                              ],
                                              MINIO_USER    : new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-username', true),
                                              MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true),
                                              dns_servers   : [
                                                      'dns_servers': [
                                                              "01": [
                                                                      cpu              : 2,
                                                                      default_user     : "zevrant",
                                                                      description      : "top level dns domain",
                                                                      gateway          : "10.0.0.1",
                                                                      hostname         : "internal-bind9",
                                                                      ip_address       : "10.0.0.8/24",
                                                                      mass_storage_name: "exosDisks",
                                                                      memory_mbs       : 4096,
                                                                      nameserver       : "1.1.1.1",
                                                                      protection       : false,
                                                                      proxmox_host     : "proxmox-01",
                                                                      ssd_storage_name : "local-zfs",
                                                                      start_on_boot    : true,
                                                                      vm_id            : 1030,
                                                              ]
                                                      ]
                                              ],
                                              bitwarden_access_token: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/bitwarden')
                            ],
                            "shared-green" : [trigger       : [
                                    type : PipelineTriggerType.UPSTREAM,
                                    value: 'shared-deploy-to-shared-common'
                            ],
                                              proxmox       : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                              VAULT_ADDR    : 'https://vault.zevrant-services.com',
                                              VAULT_TOKEN   : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                              MINIO_USER    : new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-username', true),
                                              MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true),
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