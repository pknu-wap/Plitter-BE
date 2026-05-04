package com.playlist.plitter.recommendations.application.dto;

import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.track.domain.entity.TrackEntity;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationDetailResponse(
        Long recommendationId,
        String spotifyId,
        String title,
        String artist,
        String albumCoverImageUrl,
        String previewUrl,
        List<String> comments,
        LocalDateTime createdAt
        // writer 추가 예정
) {
    public static RecommendationDetailResponse from(
            RecommendationsEntity recommendation,
            List<String> comments
    ) {
        TrackEntity track = recommendation.getTrack();

        return new RecommendationDetailResponse(
                recommendation.getId(),
                track.getSpotifyTrackId(),
                track.getTitle(),
                track.getArtistName(),
                track.getAlbumCoverUrl(),
                track.getPreviewUrl(),
                comments,
                recommendation.getCreatedAt()
        );
    }
}
