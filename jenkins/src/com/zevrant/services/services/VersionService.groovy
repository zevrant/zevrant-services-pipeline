package com.zevrant.services.services

import com.zevrant.services.pojo.Version

class VersionService extends Service {

//    private final KeydbService keydbService
//    private final PostgresService postgresService;
    private final GitService gitService

    VersionService(Object pipelineContext, boolean useK8s = true) {
        super(pipelineContext)
        gitService = new GitService(pipelineContext)
        String jenkinsExternalUrl = 'jenkins-keydb.zevrant-services.internal'
//        keydbService = new KeydbService(
//                (useK8s)? 'jenkins-versions-database-keydb' : jenkinsExternalUrl,
//                6379,
//                pipelineContext)
    }

    Version getVersion(String applicationName, boolean bareMetal = false) {
        String version = ''
//            pipelineContext.container('keydb') {
//                version = keydbService.getKey(applicationName).trim()
//            }
        if (bareMetal) {
            pipelineContext.sh """psql --csv -t -c "select version from app_version where name = '${applicationName}'" > version"""
        }
        version = pipelineContext.readFile(file: 'version')

//            version = keydbService.getKey(applicationName).trim()
        pipelineContext.println("Version not found for ${applicationName}, setting to 0.0.0")
        version = pipelineContext.readFile(file: 'version').replace('"', '').trim()
        if (version == "" || version == null || version == '(nil)') {
            version = '0.0.0'
            pipelineContext.sh("""psql -c "insert into app_version(name, version) values('${applicationName}', '0.0.0')" """)
        }

        pipelineContext.println("Version is '$version'")
        return new Version(version)
    }

    Version majorVersionUpdate(String applicationName, Version currentVersion, boolean bareMetal = false) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMajor(currentVersion.getMajor() + 1)
        version.setMedian(0);
        version.setMinor(0)
        updateVersion(version, applicationName, bareMetal)
        return version
    }

    Version medianVersionUpdate(String applicationName, Version currentVersion, boolean bareMetal = false) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMedian(currentVersion.getMedian() + 1);
        version.setMinor(0)

        updateVersion(version, applicationName, bareMetal)
        return version
    }

    Version minorVersionUpdate(String applicationName, Version currentVersion, boolean bareMetal = false) {
        Version version = new Version(currentVersion.toThreeStageVersionString());
        version.setMinor(currentVersion.getMinor() + 1)
        updateVersion(version, applicationName, bareMetal)
        return version
    }

    private void updateVersion(Version version, String applicationName, boolean bareMetal = false) {
        if (bareMetal) {
            pipelineContext.sh """psql -c "update app_version set version = '${version.toThreeStageVersionString()}' where name = '${applicationName}'" """
        }
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

    Version getPreviousVersion(Version currentVersion, isRc = false) {
        if (currentVersion.toThreeStageVersionString() == '0.0.0') {
            return currentVersion
        }
        List<String> sortedVersions = gitService.listTags()
                .collect({ tag -> tag.replace('v', '').trim() })
                .findAll({ tag -> (isRc) ? tag.matches('^\\d+\\.\\d+\\.\\d+-rc\\d+$') : tag.matches('^\\d+\\.\\d+\\.\\d+$') })
                .sort { tag1, tag2 ->
                    Version version1 = new Version(tag1)
                    Version version2 = new Version(tag2)
                    return Integer.parseInt("${version1.getMajor()}${version1.getMedian()}${version1.getMinor()}") <=> Integer.parseInt("${version2.getMajor()}${version2.getMedian()}${version2.getMinor()}")

                }
        return new Version(sortedVersions.get(sortedVersions.size() - 2))
    }

}