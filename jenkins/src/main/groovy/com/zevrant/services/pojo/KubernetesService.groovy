package com.zevrant.services.pojo

class KubernetesService {

    final String serviceName
    final boolean includesDb
    final List<KubernetesEnvironment> environments
    final String certCheckPath
    final String url

    KubernetesService(Map<String, Object> params) {
        this.serviceName = params.serviceName
        this.includesDb = (boolean) params.includesDb
        this.environments = params.environments as List<KubernetesEnvironment>
        this.certCheckPath = params.certCheckUrl ?: '/'
        this.url = params.url
    }



}
