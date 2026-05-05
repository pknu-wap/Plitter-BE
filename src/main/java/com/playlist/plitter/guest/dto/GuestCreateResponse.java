package com.playlist.plitter.guest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuestCreateResponse {
    private String guestToken;
    private String nickname;
}