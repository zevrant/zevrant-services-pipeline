package com.zevrant.services.pojo

import java.util.regex.Pattern

class Version {

    private static final List<Pattern> acceptedPatterns = [
            Pattern.compile("\\d*\\.\\d*\\.\\d*"),
            Pattern.compile("^\\d*\$")
    ]

    private int minor;
    private int median;
    private int major;

    Version(String version) {
        assert version != null: "Supplied version was null"
        assert version != "": "Supplied version was empty string"
        boolean patternMatches = false;
        for (Pattern pattern : acceptedPatterns) {
            patternMatches = patternMatches || pattern.matcher(version)
        }
        if (!patternMatches) {
            throw new RuntimeException("The supplied version does not match any of the supplied patterns");
        }
        String[] versionPieces = version.tokenize(".")
        switch (versionPieces.length) {
            case 1:
                major = Integer.parseInt(version)
                break;
            case 3:
                minor = Integer.valueOf(versionPieces[2])
                median = Integer.valueOf(versionPieces[1])
                major = Integer.valueOf(versionPieces[0])
                break;
        }

    }

    String toThreeStageVersionString() {
        return "${major}.${median}.${minor}"
    }

    String toVersionCodeString() {
        return "${major}"
    }

    int getMinor() {
        return minor
    }

    void setMinor(int minor) {
        this.minor = minor
    }

    int getMedian() {
        return median
    }

    void setMedian(int median) {
        this.median = median
    }

    int getMajor() {
        return major
    }

    void setMajor(int major) {
        this.major = major
    }
}