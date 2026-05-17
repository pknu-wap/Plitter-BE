package com.playlist.plitter.auth.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.", "USER_NOT_FOUND");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

    AuthErrorCode(final HttpStatus httpStatus, final String message, final String errorCode) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.errorCode = errorCode;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }
}
