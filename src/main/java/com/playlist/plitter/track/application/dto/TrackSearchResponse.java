package com.playlist.plitter.track.application.dto;

import java.time.LocalDateTime;

public record TrackSearchResponse(
        String trackId,
        String title,
        String artistName,
        String albumName,
        String albumCoverUrl,
        String previewUrl,
        String spotifyUrl
) {
}