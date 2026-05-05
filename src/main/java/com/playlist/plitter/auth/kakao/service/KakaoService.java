package com.playlist.plitter.auth.kakao.service;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.kakao.dto.KakaoTokenResponseDto;
import com.playlist.plitter.auth.kakao.dto.KakaoUserInfoDto;
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final KakaoUserRepository kakaoUserRepository;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.auth-url}")
    private String authUrl;

    @Value("${kakao.token-url}")
    private String tokenUrl;

    @Value("${kakao.user-info-url}")
    private String userInfoUrl;

    public String getKakaoLoginUrl() {
        return UriComponentsBuilder.fromUriString(authUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .toUriString();
    }

    public KakaoTokenResponseDto getKakaoToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        if (!clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        try {
            return RestClient.create()
                    .post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(KakaoTokenResponseDto.class);
        } catch (RestClientResponseException e) {
            throw new ApiException(KakaoErrorCode.KAKAO_TOKEN_FAILED);
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
        } catch (RestClientResponseException e) {
            throw new ApiException(KakaoErrorCode.KAKAO_USER_INFO_FAILED);
        } catch (Exception e) {
            throw new ApiException(KakaoErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    public UserEntity saveOrUpdateUser(KakaoUserInfoDto userInfo) {
        return kakaoUserRepository.findByKakaoId(userInfo.getKakaoId())
                .map(user -> {
                    user.updateNickname(userInfo.getNickname());
                    return kakaoUserRepository.save(user);
                })
                .orElseGet(() -> kakaoUserRepository.save(
                        UserEntity.builder()
                                .role(UserEntity.Role.USER)
                                .kakaoId(userInfo.getKakaoId())
                                .nickname(userInfo.getNickname())
                                .build()
                ));
    }
}
