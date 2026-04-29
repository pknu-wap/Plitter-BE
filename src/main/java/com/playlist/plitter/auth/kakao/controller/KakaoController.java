package com.playlist.plitter.auth.kakao.controller;

import com.playlist.plitter.auth.jwt.provider.JwtProvider;
import com.playlist.plitter.auth.kakao.entity.KakaoUserEntity;
import com.playlist.plitter.auth.kakao.service.KakaoService;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoController {

    private final KakaoService kakaoService;
    private final JwtProvider jwtProvider;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${jwt.refresh-token-expiration}")
    private int refreshTokenExpiration;

    @GetMapping("/login")
    public ResponseEntity<ResponseDto<String>> kakaoLogin() {
        String loginUrl = kakaoService.getKakaoLoginUrl();
        return ResponseEntity.ok(ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, loginUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code, HttpServletResponse response) {
        String kakaoAccessToken = kakaoService.getKakaoToken(code).getAccessToken();
        KakaoUserEntity user = kakaoService.saveOrUpdateUser(kakaoService.getKakaoUserInfo(kakaoAccessToken));

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(refreshTokenExpiration / 1000);
        response.addCookie(refreshTokenCookie);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "?accessToken=" + accessToken));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
