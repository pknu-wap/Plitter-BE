package com.playlist.plitter.recommendations.application;

import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.recommendations.application.dto.RecommendationCreateRequest;
import com.playlist.plitter.recommendations.application.dto.RecommendationCreateResponse;
import com.playlist.plitter.recommendations.application.dto.RecommendationDetailResponse;
import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import com.playlist.plitter.recommendations.exception.RecommendationsErrorCode;
import com.playlist.plitter.track.domain.entity.TrackEntity;
import com.playlist.plitter.track.domain.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationsService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final RecommendationsRepository recommendationsRepository;

    @Transactional
    public RecommendationCreateResponse createRecommendation(
            Long playlistId,
            RecommendationCreateRequest request
    ) {
        PlaylistEntity playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("플레이리스트를 찾을 수 없습니다."));

        if (recommendationsRepository.existsByPlaylistAndTrack_SpotifyTrackIdAndComment(
                playlist,
                request.spotifyId(),
                request.comment()
        )) {
            throw new ApiException(RecommendationsErrorCode.DUPLICATE_RECOMMENDATION);
        }

        TrackEntity track = TrackEntity.builder()
                .spotifyTrackId(request.spotifyId())
                .title(request.title())
                .artistName(request.artistName())
                .albumCoverUrl(request.albumCoverImageUrl())
                .previewUrl(request.previewUrl())
                .build();

        TrackEntity savedTrack = trackRepository.save(track);

        RecommendationsEntity recommendation = RecommendationsEntity.builder()
                .playlist(playlist)
                .track(savedTrack)
                .recommenderUser(null)
                .guestToken(null)
                .isAnonymous(false)
                .comment(request.comment())
                .build();

        RecommendationsEntity savedRecommendation = recommendationsRepository.save(recommendation);

        return new RecommendationCreateResponse(
                savedRecommendation.getId(),
                playlist.getId(),
                savedRecommendation.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public RecommendationDetailResponse getRecommendationDetail(Long recommendationId) {
        RecommendationsEntity recommendation = recommendationsRepository.findById(recommendationId)
                .orElseThrow(() -> new ApiException(RecommendationsErrorCode.RECOMMENDATION_NOT_FOUND));

        List<String> comments = recommendationsRepository.findAllByPlaylistAndTrack_SpotifyTrackId(
                        recommendation.getPlaylist(),
                        recommendation.getTrack().getSpotifyTrackId()
                )
                .stream()
                .map(RecommendationsEntity::getComment)
                .toList();

        return RecommendationDetailResponse.from(recommendation, comments);
    }
}
