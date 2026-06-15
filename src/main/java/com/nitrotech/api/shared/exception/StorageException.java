package com.nitrotech.api.shared.exception;

public class StorageException extends RuntimeException {

    private final String code;

    public StorageException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
