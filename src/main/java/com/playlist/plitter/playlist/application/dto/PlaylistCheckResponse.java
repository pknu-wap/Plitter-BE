package com.playlist.plitter.playlist.application.dto;

public record PlaylistCheckResponse(
        boolean hasPlaylist,
        Long playlistId
) {
}