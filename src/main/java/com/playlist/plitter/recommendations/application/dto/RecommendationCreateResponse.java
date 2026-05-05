package com.playlist.plitter.recommendations.application.dto;

import java.time.LocalDateTime;

public record RecommendationCreateResponse(
        Long recommendationId,
        Long playlistId,
        LocalDateTime createdAt
) {

}