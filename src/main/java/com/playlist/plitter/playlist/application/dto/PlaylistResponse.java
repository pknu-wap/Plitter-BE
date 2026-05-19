package com.playlist.plitter.playlist.application.dto;

import com.playlist.plitter.recommendations.application.dto.RecommendationResponse;

import java.util.List;

public record PlaylistResponse(
        Long playlistId,
        Integer recommendationCount,
        Boolean canCreateCharacter,
        List<RecommendationResponse> recommendations
) {
}