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
                            "shared"       : [
                                    proxmox    : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN: new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    hcp_client      : new SecretMapping(SecretType.HCP_CLIENT, ''),
                                    CF_DNS_API_TOKEN: new SecretMapping(SecretType.SECRET_TEXT, 'shared/cloudflare-dns-api-token', true),
                                    trigger: [
                                            type : PipelineTriggerType.CRON,
                                            value: "0 0 * * *"
                                    ]
                            ],
                            "shared-common": [
                                    hcp_client    : new SecretMapping(SecretType.HCP_CLIENT, ''),
                                    proxmox             : new SecretMapping(SecretType.USERNAME_PASSWORD, '/proxmox/jenkins-token'),
                                    VAULT_ADDR          : 'https://vault.zevrant-services.com',
                                    VAULT_TOKEN         : new SecretMapping(SecretType.VAULT_TOKEN, ''),
                                    "ingress_virtual_ip": "10.1.0.254/24",
                                    "pki_role"          : [ //TODO use terraform remote state instead of hardcoding
                                                            "backend"   : "pki_shared",
                                                            "issuer_ref": "7149394f-5f66-0579-fced-27db21503f89",
                                                            "name"      : "zevrant-services-shared"
                                    ],
                                    MINIO_USER    : new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-username', true),
                                    MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true),
                                    trigger: [
                                            type : PipelineTriggerType.CRON,
                                            value: "0 0 * * *"
                                    ],
                                    dns_allowed_query_cidr: "10.0.0.0/8",
                                    dns_zone_configs: [
                                            [
                                                    domain_name  : "zevrant-services.internal",
                                                    name         : "zevrant-services-shared",
                                                    nameserver_ip: "10.0.0.150",
                                                    records      : [
                                                            [
                                                                    domain    : "jenkins",
                                                                    ip_address: "10.0.0.150"
                                                            ],
                                                            [
                                                                    domain    : "plex",
                                                                    ip_address: "10.0.0.150"
                                                            ],
                                                            [
                                                                    domain    : "vault",
                                                                    ip_address: "10.0.0.150"
                                                            ],
                                                            [
                                                                    domain    : "vault",
                                                                    ip_address: "10.1.0.254"
                                                            ],
                                                            [
                                                                    domain    : "s3",
                                                                    ip_address: "10.1.0.254"
                                                            ],
                                                            [
                                                                    domain    : "s3",
                                                                    ip_address: "10.0.0.150"
                                                            ],
                                                            [
                                                                    domain    : "s3-console",
                                                                    ip_address: "10.1.0.7"
                                                            ],
                                                            [
                                                                    domain    : "garage1",
                                                                    ip_address: "10.1.0.7"
                                                            ],
                                                            [
                                                                    domain    : "garage2",
                                                                    ip_address: "10.1.0.8"
                                                            ],
                                                            [
                                                                    domain    : "garage3",
                                                                    ip_address: "10.1.0.9"
                                                            ]
                                                    ]
                                            ]
                                    ]

                            ],
                            "shared-blue"  : [
                                    trigger: [
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
                                                    description: "Keepalived haproxy",
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
                                    MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true)
                            ],
                            "shared-green" : [
                                    trigger: [
                                            type : PipelineTriggerType.UPSTREAM,
                                            value: 'shared-deploy-to-shared-common'
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
                                                    hostname         : "haproxy-shared-green",
                                                    ip_address       : "10.1.0.12/24",
                                                    is_primary       : true,
                                                    mass_storage_name: "exosDisks",
                                                    memory_mbs       : 4096,
                                                    nameserver       : "10.0.0.8",
                                                    peer_ip_addresses: [
                                                            "10.1.0.5"
                                                    ],
                                                    proxmox_host     : "proxmox-01",
                                                    replica_priority : 255,
                                                    ssd_storage_name : "local-zfs",
                                                    vm_id            : 1090
                                            ]
                                    ],
                                    MINIO_USER    : new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-username', true),
                                    MINIO_PASSWORD: new SecretMapping(SecretType.SECRET_TEXT, '/jenkins/minio-password', true)
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