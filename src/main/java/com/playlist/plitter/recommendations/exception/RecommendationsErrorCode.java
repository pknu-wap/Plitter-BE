package com.playlist.plitter.recommendations.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RecommendationsErrorCode implements ErrorCode {
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "추천된 곡을 찾을 수 없습니다.", "RECOMMENDATION_NOT_FOUND");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;

    RecommendationsErrorCode(final HttpStatus httpStatus, final String message, final String errorCode) {
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
