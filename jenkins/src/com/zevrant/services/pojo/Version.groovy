package com.zevrant.services.pojo

import java.util.regex.Pattern

class Version {

    private static final List<Pattern> acceptedPatterns = [
            Pattern.compile("\\d*\\.\\d*\\.\\d*(-\\w+)?(\\+\\w+)?"),
            Pattern.compile("^\\d*\$")
    ]

    private int patch;
    private int minor;
    private int major;
    private String prerelease;
    private String build;

    private boolean semanticVersion = false

    Version(String version) {
        assert version != null: "Supplied version was null"
        assert version.trim() != "": "Supplied version was empty string"
        boolean patternMatches = false;
        for (Pattern pattern : acceptedPatterns) {
            patternMatches = patternMatches || pattern.matcher(version)
        }
        if (!patternMatches) {
            throw new RuntimeException("The supplied version does not match any of the supplied patterns");
        }
        String[] versionPieces = version.trim().tokenize(".")
        switch (versionPieces.length) {
            case 1:
                major = Integer.parseInt(version)
                break;
            case 3:
                String patchValue = versionPieces[2]
                minor = Integer.valueOf(versionPieces[1])
                major = Integer.valueOf(versionPieces[0])

                if (patchValue.contains("-")) {
                    String preReleaseValue = patchValue.split("-")[1]
                    if (preReleaseValue.contains("+")) {
                        this.build = preReleaseValue.split("\\+")[1]
                        this.prerelease = preReleaseValue.split("\\+")[0]
                    } else {
                        this.prerelease = preReleaseValue
                    }
                }

                if (patchValue.contains("+") && !patchValue.contains("-")) {
                    this.build = patchValue.split("\\+")[1]
                    this.patch = Integer.parseInt(patchValue.split("\\+")[0])
                }

                semanticVersion = true
                break;
        }

    }

    String toSemanticVersionString() {
        if (!semanticVersion) {
            throw new RuntimeException("I'm not a semantic version")
        }
        String version = "${major}.${minor}.${patch}"
        if (prerelease != null) {
            version = "${version}-${prerelease}"
        }
        if (build != null) {
            version = "${version}+${build}"
        }
        return version
    }

    String toVersionCodeString() {
        return "${major}"
    }

    int getPatch() {
        return patch
    }

    void setPatch(int patch) {
        this.patch = patch
    }

    int getMinor() {
        return minor
    }

    void setMinor(int minor) {
        this.minor = minor
    }

    int getMajor() {
        return major
    }

    void setMajor(int major) {
        this.major = major
    }

    String getBuild() {
        return build
    }

    void setBuild(String build) {
        this.build = build
    }

    String getPrerelease() {
        return prerelease
    }

    void setPrerelease(String prerelease) {
        this.prerelease = prerelease
    }

    boolean isSemanticVersion() {
        return this.semanticVersion
    }

    void setSemanticVersion(boolean semanticVersion) {
        this.semanticVersion = semanticVersion
    }
}