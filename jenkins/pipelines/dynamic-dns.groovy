String ipAddress = ""
List<String> hostedZoneIds = []
pipeline {
    agent {
        kubernetes {
            inheritFrom 'spring-build'
        }
    }

    stages {
        stage('Pull Information') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        ipAddress = httpRequest(url: 'https://ipinfo.io/ip').content
                        sh 'aws route53 list-hosted-zones > hostedZones.json'
                        def hostedZones = readJSON(file: 'hostedZones.json')
                        List hostedZoneList = hostedZones.HostedZones.findAll({ zone -> zone.Name.contains('zevrant-services.com.') })
                        if (hostedZoneList == null || hostedZoneList.isEmpty()) {
                            throw new RuntimeException("Failed to find hosted zone for zevrant-services.com given input is ${hostedZone}")
                        }
                        hostedZoneIds = hostedZoneList
                                .collect { hostedZone ->
                                    String zoneId = hostedZone.Id
                                    println zoneId
                                    return zoneId.split("/")[2] as String }
                                .collect { hostedZoneId ->
                                    if (hostedZoneId == null || !hostedZoneId.matches('[\\w\\d]+')) {
                                        throw new RuntimeException("Hosted Zone ID ${hostedZoneId} appears to be malformed")
                                    }
                                }.findAll()

                        hostedZoneIds.each { id ->
                            sh "aws route53 list-resource-record-sets --hosted-zone-id ${id} > ${id}-recordSets.json"
                        }

                    }
                }
            }
        }

        stage('Process Record Sets') {
            steps {
                container('spring-jenkins-slave') {
                    script {
                        hostedZoneIds.each { id ->
                            def recordSets = readJSON(file: "${id}-recordSets.json")
                            def changeSet = [:]
                            changeSet['Comment'] = "Dynamic DNS Updates"
                            changeSet['Changes'] = []
                            recordSets.ResourceRecordSets
                                    .findAll({ recordSet -> recordSet.Type == 'A' })
                                    .each({ recordSet ->
                                        changeSet['Changes'].add(
                                                [
                                                        Action           : 'UPSERT',
                                                        ResourceRecordSet: [
                                                                Name           : recordSet.Name,
                                                                Type           : recordSet.Type,
                                                                TTL            : recordSet.TTL,
                                                                ResourceRecords: [
                                                                        [
                                                                                Value: ipAddress
                                                                        ]

                                                                ]
                                                        ]
                                                ]
                                        )
                                    })
                            writeJSON(file: "${id}-newRecordSets.json", json: changeSet)
                        }
                    }
                }
            }
        }

        stage('Upload Change Set') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                AWS_DEFAULT_REGION = "us-east-1"
            }
            steps {
                container('spring-jenkins-slave') {
                    script {
                        hostedZoneIds.each { id ->
                            sh "aws route53 change-resource-record-sets --hosted-zone-id ${id} --change-batch file://${id}-newRecordSets.json"
                        }

                    }
                }
            }
        }
    }
    post {
        failure {
            script {
                withCredentials([string(credentialsId: 'discord-webhook', variable: 'webhookUrl')]) {
                    discordSend description: "Jenkins Failed to Apply Dynamic DNS Updates", link: env.BUILD_URL, result: currentBuild.currentResult, title: "Dynamic DNS Update", webhookURL: webhookUrl
                }
            }
        }
    }
}
