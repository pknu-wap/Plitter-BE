package com.playlist.plitter.auth.kakao.controller;

import com.playlist.plitter.auth.kakao.service.KakaoService;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoController {

    private final KakaoService kakaoService;

    @GetMapping("/login")
    public ResponseEntity<ResponseDto<String>> kakaoLogin() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, loginUrl));
    }
}
