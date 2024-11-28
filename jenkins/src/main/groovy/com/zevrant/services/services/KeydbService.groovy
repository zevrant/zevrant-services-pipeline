package com.zevrant.services.services

class KeydbService extends Service {

    private final String host
    private final int port

    KeydbService(String host, int port, Object pipelineContext) {
        super(pipelineContext)
        this.host = host
        this.port = port
    }

    String getKey(String key) {
        return pipelineContext.sh(returnStdout: true, script: "/usr/local/bin/keydb-cli -h \'${host}\' -p ${port} get \'${key}\'")
    }

    void putKey(String key, String value) {
        pipelineContext.sh "/usr/local/bin/keydb-cli -h \'${host}\' -p ${port} set \'${key}\' \'${value}\'"
    }

}
