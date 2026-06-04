package com.playlist.plitter.playlist.application.dto;

public record PlaylistCreateResponse(
        Long playlistId,
        String publicShareId,
        String shareUrl
) {
}
