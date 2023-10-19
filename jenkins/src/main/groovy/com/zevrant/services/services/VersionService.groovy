package com.zevrant.services.services

import com.zevrant.services.pojo.Version

class VersionService extends Service {
    
    VersionService(Object pipelineContext) {
        super(pipelineContext)
    }

    Version getVersion(String applicationName) {
        Object parametersResponse = pipelineContext.readJSON(text: (pipelineContext.sh(returnStdout: true, script: 'aws ssm describe-parameters') as String))
        boolean containsParameter = false;
        for (Object parameter : parametersResponse.Parameters) {
            containsParameter = containsParameter || parameter.Name.contains("${applicationName}-VERSION")
        }
        String version = ""
        if (containsParameter) {
            version = pipelineContext.readJSON(text: pipelineContext.sh(returnStdout: true, script: "aws ssm --name get-parameter ${applicationName}-VERSION")).Parameter.Value
        } else {
            version = "0.0.0"
        }
        return new Version(version)
    }

    Version majorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMajor(currentVersion.getMajor() + 1)
        version.setMedian(0);
        version.setMinor(0)
        pipelineContext.sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
        return version
    }

    Version medianVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMedian(currentVersion.getMedian() + 1);
        version.setMinor(0)

        pipelineContext.sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
        return version
    }

    Version minorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMinor(currentVersion.getMinor() + 1)

        pipelineContext.sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toThreeStageVersionString()} --type String --overwrite"
        return version
    }

    Version incrementVersion(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        pipelineContext.sh "aws ssm put-parameter --name ${applicationName}-VERSION --value ${version.toVersionCodeString()} --type String --overwrite"
        return version
    }

    Version incrementVersionCode(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        pipelineContext.sh "aws ssm put-parameter --name ${applicationName}-code-VERSION --value ${version.toVersionCodeString()} --type String --overwrite"
        return version
    }

    Version getVersionCode(String applicationName) {
        return getVersion("$applicationName-code")
    }
}