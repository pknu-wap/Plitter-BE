package com.playlist.plitter.auth.kakao.exception;

import com.playlist.plitter.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum KakaoErrorCode implements ErrorCode {

    KAKAO_TOKEN_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_TOKEN_FAILED", "카카오 토큰 발급에 실패했습니다"),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_USER_INFO_FAILED", "카카오 유저 정보 조회에 실패했습니다");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getErrorCode() { return errorCode; }

    @Override
    public String getMessage() { return message; }
}
