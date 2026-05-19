package com.playlist.plitter.recommendations.application.dto;

import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.track.domain.entity.TrackEntity;

import java.util.List;

public record RecommendationDetailResponse(
        Long recommendationId,
        String spotifyId,
        String title,
        String artist,
        String albumCoverImageUrl,
        String previewUrl,
        List<CommentResponse> comments
) {
    public static RecommendationDetailResponse from(
            RecommendationsEntity recommendation,
            List<CommentResponse> comments
    ) {
        TrackEntity track = recommendation.getTrack();

        return new RecommendationDetailResponse(
                recommendation.getId(),
                track.getSpotifyTrackId(),
                track.getTitle(),
                track.getArtistName(),
                track.getAlbumCoverUrl(),
                track.getPreviewUrl(),
                comments
        );
    }

    public record CommentResponse(
            String recommenderName,
            String comment
    ) {
        public static CommentResponse from(RecommendationsEntity recommendation) {
            String recommenderName = recommendation.getRecommenderUser() != null
                    ? recommendation.getRecommenderUser().getNickname()
                    : recommendation.getRandomNickname();

            return new CommentResponse(recommenderName, recommendation.getComment());
        }
    }
}
