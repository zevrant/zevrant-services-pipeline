package com.zevrant.services.services

class MinioService extends Service {

    MinioService(Object pipelineContext) {
        super(pipelineContext)
    }

    void withContext(String credentialsId, String objectStoreUrl = 'https://minio-shared.zevrant-services.internal', Closure closure) {
        pipelineContext.sh 'mc alias set default '
                .concat(objectStoreUrl)
                .concat(' "$ACCESS_KEY_ID" "$SECRET_ACCESS_KEY"')
        try {
            closure.call()
        } finally {
            pipelineContext.sh 'mc alias remove default'
        }
    }


    void uploadFile(String bucket, String source, String destination) {
        pipelineContext.sh "mc cp ${source} default/${bucket}/${destination}"
    }

    void getFile(String bucket, String source, String destination) {
        pipelineContext.sh "mc cp default/${bucket}/${source} ${destination}"
    }
}
