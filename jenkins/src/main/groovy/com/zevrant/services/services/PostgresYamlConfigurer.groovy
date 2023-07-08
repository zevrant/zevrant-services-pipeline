package com.zevrant.services.services

String configurePostgresHelmChart(String appName, String ipAddress) {
    def response = httpRequest(
            url: 'https://raw.githubusercontent.com/bitnami/charts/main/bitnami/postgresql-ha/values.yaml'
    )
    
    def values = readYaml(text: response.content)

//    values.global.imagePullSecrets = ['harbor-ro']
    values.global.storageClass = 'csi-rbd-sc'
    values.global.postgresql.username = 'zevrant'
    values.global.postgresql.database = "backup"
    values.global.postgresql.repmgrDatabase = "backup"
//    values.global.postgresql.existingSecret = "backup-service-postgres-credentials"
//    values.postgresql.image.registry = "harbor.zevrant-services.com"
//    values.postgresql.image.repository = "dockerhub/bitnami/postgresql-repmgr"
//    values.postgresql.resources.limits.cpu = 2
//    values.postgresql.resources.limits.memory = "4Gi"
//    values.postgresql.resources.requests.cpu = '1000m'
//    values.postgresql.resources.requests.memory = '2Gi'
//    values.postgresql.syncReplicationMode = 'ANY'
//    values.postgresql.syncReplication = true
//    values.postgresql.tls.enabled = true
//    values.postgresql.tls.preferServerCiphers = true
//    values.postgresql.tls.certificatesSecret = "${appName}-postgres-tls"
//    values.postgresql.tls.certFilename = 'tls.crt'
//    values.postgresql.tls.certKeyFilename = 'tls.key'
//    values.pgpool.image.registry = 'harbor.zevrant-services.com'
//    values.pgpool.image.repository = 'dockerhub/bitnami/pgpool'
//    values.pgpool.replicaCount = 3
//    values.pgpool.resources.limits.cpu = '500m'
//    values.pgpool.resources.limits.memory = '512Mi'
//    values.pgpool.resources.requests.cpu = '250m'
//    values.pgpool.resources.requests.memory = '256Mi'
//    values.pgpool.reservedConnections = 4
//    values.pgpool.tls.enabled = true
//    values.pgpool.tls.autogenerated = false
//    values.pgpool.tls.preferServerCiphers = true
//    values.pgpool.tls.certificatesSecret = "${appName}-postgres-tls"
//    values.pgpool.tls.certFilename = 'tls.crt'
//    values.pgpool.tls.certKeyFilename = 'tls.key'
//    values.metrics.image.registry = 'harbor.zevrant-services.com'
//    values.metrics.image.repository = 'dockerhub/bitnami/postgres-exporter'
//    values.volumePermissions.image.registry = 'harbor.zevrant-services.com'
//    values.volumePermissions.image.repository = 'dockerhub/bitnami/bitnami-shell'
//    values.persistence.storageClass = 'csi-rbd-sc'
//    values.persistence.size = '50Gi'
//    values.service.clusterIP = ipAddress
    return writeYaml(returnText: true, data: values)
}

