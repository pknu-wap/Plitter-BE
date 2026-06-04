package com.playlist.plitter.playlist.application;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.auth.exception.AuthErrorCode;
import com.playlist.plitter.playlist.application.dto.PlaylistCheckResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistPublicResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.recommendations.application.dto.RecommendationResponse;
import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import com.playlist.plitter.track.domain.entity.TrackEntity;
import com.playlist.plitter.playlist.exception.PlaylistErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final int REQUIRED_RECOMMENDATION_COUNT_FOR_CHARACTER = 5;

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final RecommendationsRepository recommendationsRepository;

    public PlaylistCreateResponse savePlaylist(Long userId) {

        System.out.println("* 유저 아이디: " + userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(AuthErrorCode.USER_NOT_FOUND));

        if (playlistRepository.existsByOwner(user)) {
            throw new ApiException(PlaylistErrorCode.PLAYLIST_ALREADY_EXISTS);
        }

        String shortId = UUID.randomUUID().toString().substring(0, 5);

        PlaylistEntity playlist = PlaylistEntity.builder()
                .owner(user)
                .shortId(shortId)
                .characterVersion(0)
                .build();

        PlaylistEntity saved = playlistRepository.save(playlist);

        Long playlistId = saved.getId();
        String shareUrl = "https://ourdomain.com/playlist/" + saved.getShortId();

        return new PlaylistCreateResponse(playlistId, shortId, shareUrl);
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(Long playlistId) {
        PlaylistEntity playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ApiException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        List<RecommendationResponse> recommendations = toRecommendationResponses(playlist);
        int recommendationCount = recommendations.size();

        return new PlaylistResponse(
                playlist.getId(),
                recommendationCount,
                recommendationCount >= REQUIRED_RECOMMENDATION_COUNT_FOR_CHARACTER,
                recommendations
        );
    }

    public PlaylistCheckResponse checkPlaylist(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Optional<PlaylistEntity> playlist = playlistRepository.findByOwnerId(userId);

        if (playlist.isPresent()) {
            return new PlaylistCheckResponse(true, playlist.get().getId());
        }

        return new PlaylistCheckResponse(false, null);
    }

    @Transactional(readOnly = true)
    public PlaylistPublicResponse getPlaylistPublic(Long playlistId) {
        PlaylistEntity playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ApiException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        return toPlaylistPublicResponse(playlist);
    }

    @Transactional(readOnly = true)
    public PlaylistPublicResponse getPlaylistPublicByShortId(String shortId) {
        PlaylistEntity playlist = playlistRepository.findByShortId(shortId)
                .orElseThrow(() -> new ApiException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        return toPlaylistPublicResponse(playlist);
    }

    private PlaylistPublicResponse toPlaylistPublicResponse(PlaylistEntity playlist) {
        List<RecommendationResponse> recommendations = toRecommendationResponses(playlist);
        String latestCoverImageUrl = recommendationsRepository.findTopByPlaylistOrderByCreatedAtDescIdDesc(playlist)
                .map(RecommendationsEntity::getTrack)
                .map(TrackEntity::getAlbumCoverUrl)
                .orElse(null);

        int recommendationCount = recommendations.size();
        return new PlaylistPublicResponse(
                playlist.getId(),
                playlist.getShortId(),
                recommendationCount,
                recommendationCount >= REQUIRED_RECOMMENDATION_COUNT_FOR_CHARACTER,
                playlist.getOwner().getNickname(),
                recommendations,
                latestCoverImageUrl
        );
    }

    private List<RecommendationResponse> toRecommendationResponses(PlaylistEntity playlist) {
        List<RecommendationsEntity> recommendations = recommendationsRepository.findAllByPlaylist(playlist);
        Map<String, Long> commentCountBySpotifyId = recommendations.stream()
                .map(recommendation -> recommendation.getTrack().getSpotifyTrackId())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return recommendations
                .stream()
                .map(recommendation -> {
                    TrackEntity track = recommendation.getTrack();
                    return new RecommendationResponse(
                            recommendation.getId(),
                            track.getSpotifyTrackId(),
                            track.getTitle(),
                            track.getArtistName(),
                            track.getAlbumCoverUrl(),
                            track.getPreviewUrl(),
                            commentCountBySpotifyId.getOrDefault(track.getSpotifyTrackId(), 0L).intValue()
                    );
                })
                .toList();
    }

}
