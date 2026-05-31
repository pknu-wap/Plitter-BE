package com.playlist.plitter.playlist.application.dto;

public record PlaylistPublicResponse(
        Long playlistId,
        String shortId,
        Integer recommendationCount,
        Boolean canCreateCharacter,
        String latestCoverImageUrl
) {
}
