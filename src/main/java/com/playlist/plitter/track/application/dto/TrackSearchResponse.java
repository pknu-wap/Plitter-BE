package com.playlist.plitter.track.application.dto;

public record TrackSearchResponse(
        String spotifyId,
        String title,
        String artistName,
        String albumCoverImageUrl,
        String previewUrl,
        String spotifyUrl
) {
}