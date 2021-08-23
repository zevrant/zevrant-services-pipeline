package com.zevrant.services

enum ApplicationType {
    LIBRARY('Libraries'),
    ANDROID('Android');

    final String value;

    ApplicationType(String value) {
        this.value = value
    }
}