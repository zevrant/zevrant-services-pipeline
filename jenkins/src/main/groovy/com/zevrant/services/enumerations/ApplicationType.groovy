package com.zevrant.services.enumerations

enum ApplicationType {
    JAVA_LIBRARY('Libraries', 'jenkins/pipelines/java-library-build.groovy'),
    ANDROID('Android', 'jenkins/pipelines/android-build.groovy'),
    SPRING('Spring', 'jenkins/pipelines/spring-build.groovy'),
    JENKINS_CAC('Admin Utilities', 'jenkins/pipelines/admin/cacUpdate.groovy')

    private final String value;
    private final String remoteJenkinsfile;

    ApplicationType(String value, String remoteJenkinsfile) {
        this.value = value
        this.remoteJenkinsfile = remoteJenkinsfile
    }

    String getValue() {
        return value
    }

    String getRemoteJenkinsfile() {
        return remoteJenkinsfile
    }
}