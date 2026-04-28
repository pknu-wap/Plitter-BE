package com.playlist.plitter.auth.kakao.service;

import com.playlist.plitter.auth.kakao.dto.KakaoTokenResponseDto;
import com.playlist.plitter.auth.kakao.dto.KakaoUserInfoDto;
import com.playlist.plitter.auth.kakao.entity.KakaoUserEntity;
import com.playlist.plitter.auth.kakao.exception.KakaoErrorCode;
import com.playlist.plitter.auth.kakao.repository.KakaoUserRepository;
import com.playlist.plitter.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoUserRepository kakaoUserRepository;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.auth-url}")
    private String authUrl;

    @Value("${kakao.token-url}")
    private String tokenUrl;

    @Value("${kakao.user-info-url}")
    private String userInfoUrl;

    public String getKakaoLoginUrl() {
        return authUrl
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri;
    }

    public KakaoTokenResponseDto getKakaoToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            return RestClient.create()
                    .post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(KakaoTokenResponseDto.class);
        } catch (Exception e) {
            throw new ApiException(KakaoErrorCode.KAKAO_TOKEN_FAILED);
        }
    }

    public KakaoUserInfoDto getKakaoUserInfo(String accessToken) {
        try {
            return RestClient.create()
                    .get()
                    .uri(userInfoUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoDto.class);
        } catch (Exception e) {
            throw new ApiException(KakaoErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    public KakaoUserEntity saveOrUpdateUser(KakaoUserInfoDto userInfo) {
        return kakaoUserRepository.findByKakaoId(userInfo.getKakaoId())
                .map(user -> {
                    user.updateNickname(userInfo.getNickname());
                    return kakaoUserRepository.save(user);
                })
                .orElseGet(() -> kakaoUserRepository.save(
                        KakaoUserEntity.builder()
                                .kakaoId(userInfo.getKakaoId())
                                .nickname(userInfo.getNickname())
                                .build()
                ));
    }
}