package com.zevrant.services.services

class HashingService extends Service {

    HashingService(Object pipelineContext) {
        super(pipelineContext)
    }

    public String getSha512SumFor(String filePath) {
        if (filePath.contains(';') || filePath.contains('|') || filePath.contains('&')) {
            throw new RuntimeException("The special characters '&', ';', and '|' are not allowed in the provided file path")
        }
        pipelineContext.sh("sha512sum ${filePath.trim()} > sha512sum")
        String sha512Sum = pipelineContext.readFile(file: 'sha512sum')
        pipelineContext.sh('rm sha512sum')
        return sha512Sum
    }
}
