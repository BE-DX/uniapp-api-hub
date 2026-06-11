package com.uniapp.apihub.module.auth;

import lombok.Getter;

import java.util.Map;

@Getter
public class LoginConflictException extends RuntimeException {

    private final Map<String, Object> data;

    public LoginConflictException(String message, Map<String, Object> data) {
        super(message);
        this.data = data;
    }
}
