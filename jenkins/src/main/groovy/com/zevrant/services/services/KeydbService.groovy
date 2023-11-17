package com.zevrant.services.services

class KeydbService extends Service {

    private final String host
    private final int port

    KeydbService(String host, int port, Object pipelineContext) {
        super(pipelineContext)
    }

    String getKey(String key) {
        return pipelineContext.sh(returnStdout: true, script: "keydb-cli -h \'${host}\' -p ${port} get \'${key}\'")
    }

    void putKey(String key, String value) {
        pipelineContext.sh "keydb-cli -h \'${host}\' -p ${port} set \'${key}\' \'${value}\'"
    }

}
