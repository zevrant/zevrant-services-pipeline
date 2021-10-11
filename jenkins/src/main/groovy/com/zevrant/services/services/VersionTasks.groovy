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

void majorVersionUpdate(String appName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    version.setMajor(currentVersion.getMajor() + 1)
    version.setMedian(0);
    version.setMinor(0)
    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
}

Version medianVersionUpdate(String appName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    version.setMedian(currentVersion.getMedian() + 1);
    version.setMinor(0)

    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}

Version minorVersionUpdate(String appName, Version currentVersion) {
    Version version = new Version(currentVersion.toThreeStageVersionString());
    currentVersion.setMinor(currentVersion.getMinor() + 1)

    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}

Version incrementVersionCode(String appName, Version currentVersion) {
    Version version = new Version(currentVersion.toVersionCodeString())
    sh "aws ssm put-parameter --name ${appName}-code-VERSION --value ${version.toVersionCodeString()} --type String --overwrite"
    return currentVersion
}

Version getVersionCode(String appName) {
    return getVersion("$appName-code")
}