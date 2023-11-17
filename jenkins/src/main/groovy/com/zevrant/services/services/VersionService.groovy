package com.zevrant.services.services

import com.zevrant.services.pojo.Version

class VersionService extends Service {

    private final KeydbService keydbService

    VersionService(Object pipelineContext, boolean useK8s = true) {
        super(pipelineContext)
        String jenkinsExternalUrl = 'jenkins-keydb.zevrant-services.internal'
        keydbService = new KeydbService(
                (useK8s)? 'jenkins-versions-database-keydb' : jenkinsExternalUrl,
                6379,
                pipelineContext)
    }

    Version getVersion(String applicationName) {
        String version = ''
        try {
            pipelineContext.container('keydb') {
                version = keydbService.getKey(applicationName).trim()
            }
        } catch (Exception ignored) {
            version = keydbService.getKey(applicationName).trim()
        }
        version = version.trim()
        version = (version == "" || version == null) ? '0.0.0' : version
        return new Version(version)
    }

    Version majorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMajor(currentVersion.getMajor() + 1)
        version.setMedian(0);
        version.setMinor(0)
        try {
            pipelineContext.container('keydb') {
                keydbService.putKey(applicationName, version.toThreeStageVersionString())
            }
        } catch (Exception ignored) {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version medianVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMedian(currentVersion.getMedian() + 1);
        version.setMinor(0)

        try {
            pipelineContext.container('keydb') {
                keydbService.putKey(applicationName, version.toThreeStageVersionString())
            }
        } catch (Exception ignored) {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version minorVersionUpdate(String applicationName, Version currentVersion) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMinor(currentVersion.getMinor() + 1)

        try {
            pipelineContext.container('keydb') {
                keydbService.putKey(applicationName, version.toThreeStageVersionString())
            }
        } catch (Exception ignored) {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version incrementVersion(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        try {
            pipelineContext.container('keydb') {
                keydbService.putKey(applicationName, version.toVersionCodeString())
            }
        } catch (Exception ignored) {
            keydbService.putKey(applicationName, version.toThreeStageVersionString())
        }
        return version
    }

    Version incrementVersionCode(String applicationName, Version currentVersion) {
        Version version = new Version(String.valueOf(Integer.parseInt(currentVersion.toVersionCodeString()) + 1))
        try {
            pipelineContext.container('keydb') {
                keydbService.putKey(applicationName, version.toVersionCodeString())
            }
        } catch (Exception ignored) {
            keydbService.putKey(applicationName, version.toVersionCodeString())
        }
        return version
    }

}