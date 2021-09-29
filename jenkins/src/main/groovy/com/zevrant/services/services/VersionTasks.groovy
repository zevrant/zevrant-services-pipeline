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
        version = readJSON(text: sh(returnStdout: true, script: "aws ssm --name get-parameter ${applicationName}-VERSION")).Value
    } else {
        version = "0.0.0"
    }
    return new Version(version)
}

Version majorVersionUpdate(String appName, Version currentVersion) {
    currentVersion.setMajor(currentVersion.getMajor() + 1)
    currentVersion.setMedian(0);
    currentVersion.setMinor(0)

    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${currentVersion.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}

Version medianVersionUpdate(String appName, Version currentVersion) {
    currentVersion.setMedian(currentVersion.getMedian() + 1);
    currentVersion.setMinor(0)

    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${currentVersion.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}

Version minorVersionUpdate(String appName, Version currentVersion) {
    currentVersion.setMinor(currentVersion.getMinor() + 1)

    sh "aws ssm put-parameter --name ${appName}-VERSION --value ${currentVersion.toThreeStageVersionString()} --type String --overwrite"
    return currentVersion
}