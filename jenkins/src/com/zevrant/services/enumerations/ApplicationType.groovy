package com.zevrant.services.enumerations

enum ApplicationType {
    ANDROID('Android', 'jenkins/pipelines/android-build.groovy'),
    ANGULAR('Angular', 'jenkins/pipelines/angular/angular-build.groovy'),
    GO('GO', 'jenkins/pipelines/go/build-go-app.groovy'),
    JAVA_LIBRARY('Libraries', 'jenkins/pipelines/java-library-build.groovy'),
    JENKINS_CAC('Admin Utilities', 'jenkins/pipelines/admin/cacUpdate.groovy'),
    TERRAFORM('Terraform', 'jenkins/pipelines/terraform/build-test-terraform.groovy'),
    TERRAFORM_PROVIDER('Terraform Provider', 'jenkins/pipelines/go/build-go-app-baremetal.groovy'),
    SPRING('Spring', 'jenkins/pipelines/spring/artifact-build.groovy')

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