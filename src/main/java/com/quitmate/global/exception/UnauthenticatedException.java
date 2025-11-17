package com.quitmate.global.exception;

import lombok.Getter;

@Getter
public class UnauthenticatedException extends RuntimeException {
    private final String message;
    public UnauthenticatedException(String message, Exception e) {
        super(message, e);
        this.message = message;
    }
    public UnauthenticatedException(String message) {
        this.message = message;
    }
}