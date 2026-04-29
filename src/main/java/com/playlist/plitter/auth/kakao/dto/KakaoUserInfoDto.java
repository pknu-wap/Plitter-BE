package com.playlist.plitter.auth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoDto {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public String getKakaoId() {
        return String.valueOf(id);
    }

    public String getNickname() {
        return kakaoAccount.getProfile().getNickname();
    }

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        private Profile profile;

        @Getter
        @NoArgsConstructor
        public static class Profile {
            private String nickname;
        }
    }
}
