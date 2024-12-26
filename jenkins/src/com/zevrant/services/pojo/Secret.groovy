package com.zevrant.services.pojo

class Secret {

    private final String username
    private final String password

    Secret(String username, String password) {
        this.username = username
        this.password = password
    }

    String getUsername() {
        return username
    }

    String getPassword() {
        return password
    }
}
