package com.zevrant.services.services

import com.zevrant.services.pojo.Version

class VersionService extends Service {

    private final KeydbService keydbService

    VersionService(Object pipelineContext) {
        super(pipelineContext)
        keydbService = new KeydbService('jenkins-versions-database-keydb', 6379, pipelineContext)
    }

    Version getVersion(String applicationName) {
        String version = ''
        pipelineContext.container('keydb') {
            version = keydbService.getKey(applicationName).trim()
        }
        version = (version == "" || version == null)? '0.0.0' : version
        return new Version(version)
    }

    Version majorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMajor(currentVersion.getMajor() + 1)
        version.setMedian(0);
        version.setMinor(0)
        pipelineContext.container('keydb') {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version medianVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMedian(currentVersion.getMedian() + 1);
        version.setMinor(0)

        pipelineContext.container('keydb') {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version minorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMinor(currentVersion.getMinor() + 1)

        pipelineContext.container('keydb') {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version incrementVersion(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        pipelineContext.container('keydb') {
            keydbService.putKey(applicationName, version.toVersionCodeString())
        }
        return version
    }

    Version incrementVersionCode(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        pipelineContext.container('keydb') {
            keydbService.putKey(applicationName, version.toVersionCodeString())
        }
        return version
    }

}