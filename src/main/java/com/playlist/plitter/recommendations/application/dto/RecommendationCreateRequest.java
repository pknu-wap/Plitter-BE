package com.playlist.plitter.recommendations.application.dto;

public record RecommendationCreateRequest(
        String spotifyId,
        String title,
        String artistName,
        String albumCoverImageUrl,
        String previewUrl,
        String comment

) {

}
