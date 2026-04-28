package com.playlist.plitter.track.application;

import com.playlist.plitter.track.application.dto.TrackSearchResponse;
import com.playlist.plitter.track.infrastructure.spotify.SpotifyTrackClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final SpotifyTrackClient spotifyTrackClient;

    public List<TrackSearchResponse> searchTracks(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }

        return spotifyTrackClient.searchTracks(keyword, limit);
    }
}
