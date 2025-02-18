package com.zevrant.services.pojo

class UsernamePasswordSecret {

    private final String username;
    private final String password;

    UsernamePasswordSecret(String username, String password) {
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
