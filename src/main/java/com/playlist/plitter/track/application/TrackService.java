package com.playlist.plitter.track.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final SpotifyTokenService spotifyTokenService;

    public void someMethod() {
        String accessToken = spotifyTokenService.createAccessToken();
        // accessToken으로 Spotify API 호출
    }
}