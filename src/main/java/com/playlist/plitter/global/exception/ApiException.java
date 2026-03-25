package com.playlist.plitter.global.exception;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
