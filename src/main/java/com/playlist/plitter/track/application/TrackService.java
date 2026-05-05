package com.playlist.plitter.track.application;

import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.track.application.dto.TrackSearchResponse;
import com.playlist.plitter.track.exception.TrackErrorCode;
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
            throw new ApiException(TrackErrorCode.INVALID_REQUEST);
        }

        try {
            return spotifyTrackClient.searchTracks(keyword, limit);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(TrackErrorCode.TRACK_SEARCH_FAILED);

        }
    }
}
