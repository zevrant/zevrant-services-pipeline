package com.zevrant.services.enumerations

enum ApplicationType {
    LIBRARY('Libraries'),
    ANDROID('Android'),
    SPRING('Spring');

    final String value;

    ApplicationType(String value) {
        this.value = value
    }
}