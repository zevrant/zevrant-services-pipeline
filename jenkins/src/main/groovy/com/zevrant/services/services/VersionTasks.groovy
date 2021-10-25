package com.zevrant.services.services

import com.zevrant.services.pojo.Version

Version getVersion(String applicationName) {
    Object parametersResponse = readJSON(text: (sh(returnStdout: true, script: 'aws ssm describe-parameters') as String))
    boolean containsParameter = false;
    for (Object parameter : parametersResponse.Parameters) {
        containsParameter = containsParameter || parameter.Name.contains("${applicationName}-VERSION")
    }
    String version = ""
    if (containsParameter) {
        version = readJSON(text: sh(returnStdout: true, script: "aws ssm --name get-parameter ${applicationName}-VERSION")).Parameter.Value
    } else {
        version = "0.0.0"
    }
    return new Version(version)
}

void majorVersionUpdate(String applicationName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    version.setMajor(currentVersion.getMajor() + 1)
    version.setMedian(0);
    version.setMinor(0)
    sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
}

Version medianVersionUpdate(String applicationName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    version.setMedian(currentVersion.getMedian() + 1);
    version.setMinor(0)

    sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}

Version minorVersionUpdate(String applicationName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    version.setMinor(currentVersion.getMinor() + 1)

    sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}
version

Version incrementVersionCode(String applicationName, Version currentVersion) {
    Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
    sh "aws ssm put-parameter --name ${applicationName}-code-VERSION --value ${version.toVersionCodeString()} --type String --overwrite"
    return currentVersion
}

Version getVersionCode(String applicationName) {
    return getVersion("$applicationName-code")
}

Version getPreviousVersion(String applicationName) {
    def parameterVersions = readJSON(text: sh(returnStdout: true, script: "aws ssm --name get-parameter-history ${applicationName}-VERSION")).Parameters
    String currentParamVersion = ""
    String currentVersion = readJSON(text: sh(returnStdout: true, script: "aws ssm --name get-parameter ${applicationName}-VERSION")).Parameter.Value
    for (int i = 0; i < parameterVersions.length; i++) {
        if (currentVersion == parameterVersions[i].Value) {
            currentParamVersion = parameterVersions[i].Version
        }
    }
    for (int i = 0; i < parameterVersions.length; i++) {
        if (String.valueOf(Integer.parseInt(currentParamVersion) - 1) == parameterVersions[i].Value) {
            return new Version(parameterVersions[i].Value as String)
        }
    }
    throw new RuntimeException("Previous version not found")
}