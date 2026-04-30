package com.playlist.plitter.character.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CharacterErrorCode implements ErrorCode {
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND", "플레이리스트를 찾을 수 없습니다."),
    CHARACTER_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CHARACTER_NOT_AVAILABLE", "캐릭터 생성 조건을 만족하지 않습니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", "생성된 캐릭터를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;

    CharacterErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
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
