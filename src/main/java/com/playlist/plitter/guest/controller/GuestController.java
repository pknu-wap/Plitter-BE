package com.playlist.plitter.guest.controller;

import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.guest.dto.GuestCreateResponse;
import com.playlist.plitter.guest.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PostMapping("/guest")
    public ResponseEntity<ResponseDto<GuestCreateResponse>> createGuest() {
        GuestCreateResponse response = guestService.createGuest();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDto.ofSuccess(SuccessMessage.CREATE_SUCCESS, response));
    }
}