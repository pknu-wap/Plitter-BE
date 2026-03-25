package com.playlist.plitter.global.dto;

public enum SuccessMessage {
    OPERATION_SUCCESS("요청이 성공적으로 처리되었습니다."),
    CREATE_SUCCESS("등록이 완료되었습니다."),
    UPDATE_SUCCESS("수정이 완료되었습니다."),
    DELETE_SUCCESS("삭제가 완료되었습니다.");


    private final String message;

    SuccessMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
