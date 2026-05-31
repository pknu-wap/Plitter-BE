package com.playlist.plitter.playlist.application.dto;

import com.playlist.plitter.recommendations.application.dto.RecommendationResponse;

import java.util.List;

public record PlaylistPublicResponse(
        Long playlistId,
        String shortId,
        Integer recommendationCount,
        Boolean canCreateCharacter,
        String ownerNickname,
        List<RecommendationResponse> recommendations,
        String latestCoverImageUrl
) {
}
