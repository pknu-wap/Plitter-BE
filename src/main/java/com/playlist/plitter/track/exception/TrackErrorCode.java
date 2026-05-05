package com.playlist.plitter.track.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TrackErrorCode implements ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "검색어는 필수입니다.", "INVALID_REQUEST"),
    SPOTIFY_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Spotify 인증에 실패했습니다.", "SPOTIFY_UNAUTHORIZED"),
    TRACK_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "노래 검색 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

    TrackErrorCode(final HttpStatus httpStatus, final String message, final String errorCode) {
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