package com.playlist.plitter.recommendations.application.dto;

public record RecommendationResponse(
        Long recommendationId,
        String spotifyId,
        String title,
        String artistName,
        String albumCoverImageUrl,
        String previewUrl,
        Integer commentCount
) {
}
