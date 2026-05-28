package com.playlist.plitter.track.application.dto;

public record TrackPlayResponse(
        String spotifyTrackId,
        String embedUrl
) {
}