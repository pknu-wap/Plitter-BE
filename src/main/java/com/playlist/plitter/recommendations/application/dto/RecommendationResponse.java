package com.playlist.plitter.recommendations.application.dto;

public record RecommendationResponse(
        String spotifyId,
        String title,
        String artistName,
        String albumCoverImageUrl,
        String previewUrl
) {
}
