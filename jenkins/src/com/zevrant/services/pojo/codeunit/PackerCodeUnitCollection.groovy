package com.zevrant.services.pojo.codeunit

import com.zevrant.services.pojo.GitRepo

class PackerCodeUnitCollection {

    public static final List<PackerCodeUnit> packerImages = Collections.unmodifiableList([
            new PackerCodeUnit(
                    name: 'alma-base-image',
                    extraArguments: [
                            'nodeExporterVersion': '1.8.2',
                    ],
                    folderPath: 'base-vm',
                    repo: new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-pipeline', 'jenkins-git'
                    )
            ),
            new PackerCodeUnit(
                    name: 'jenkins-agent',
                    baseImageName: 'alma-base-image',
                    extraArguments: [
                            'jenkinsSshKey'          : 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIO9n652oHq/eI9F2EUI0xq2ZZw9pgkeQU8+h3HXDU1H/ zevrant@zevrant',
                            'jenkinsAgentServiceFile': ''
                    ],
                    folderPath: 'jenkins-agent',
                    repo: new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-pipeline', 'jenkins-git'
                    )
            ),
            new PackerCodeUnit(
                    name: 'jenkins',
                    baseImageName: 'alma-base-image',
                    folderPath: 'jenkins',
                    repo: new GitRepo(
                            'github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-pipeline', 'jenkins-git'
                    )
            ),
            new PackerCodeUnit(
                    name: 'nginx-base',
                    baseImageName: 'alma-base-image',
                    folderPath: 'nginx-base',
                    repo: new GitRepo('github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-pipeline', 'jenkins-git'),
            ),
            new PackerCodeUnit(
                    name: 'shared-nginx-ingress',
                    baseImageName: 'nginx-base',
                    folderPath: 'shared-nginx-ingress',
                    repo: new GitRepo('github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-pipeline', 'jenkins-git'),
                    specRepo: new GitRepo('github.com', 'git@github.com',
                            'zevrant', 'zevrant-services-terraform', 'jenkins-git'),
                    extraArguments: [
                            firewall_ports: [
                                    [
                                            protocol: 'tcp',
                                            port    : '80'
                                    ]
                            ],
                            nginx_configs : [
                                    [
                                            src : 'nginx.conf',
                                            dest: '/etc/nginx/nginx.conf'
                                    ],
                                    [
                                            src : 'jenkins-ingress.conf',
                                            dest: '/etc/nginx/conf.d/http/jenkins-ingress.conf'
                                    ]
                            ]
                    ]
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
