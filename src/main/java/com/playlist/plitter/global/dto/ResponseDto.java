package com.playlist.plitter.global.dto;

public record ResponseDto<D>(
        String code,
        String message,
        D content
) {
    private static final String SUCCESS_CODE = "SUCCESS";

    public static <D> ResponseDto<D> ofSuccess(SuccessMessage success, D data) {
        return new ResponseDto<>(SUCCESS_CODE, success.getMessage(), data);
    }

    public static ResponseDto<Void> ofSuccess(SuccessMessage success) {
        return new ResponseDto<>(SUCCESS_CODE, success.getMessage(), null);
    }

    public static ResponseDto<Void> ofFailure(String errorCode, String message) {
        return new ResponseDto<>(errorCode, message, null);
    }
}
