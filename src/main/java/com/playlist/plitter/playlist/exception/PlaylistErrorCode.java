package com.playlist.plitter.playlist.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PlaylistErrorCode implements ErrorCode {
    PLAYLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "플레이리스트가 이미 존재합니다.", "PLAYLIST_ALREADY_EXISTS");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

    PlaylistErrorCode(final HttpStatus httpStatus, final String message, final String errorCode) {
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