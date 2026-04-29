package com.playlist.plitter.playlist.application;

import com.playlist.plitter.playlist.application.dto.PlaylistCheckResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.exception.PlaylistErrorCode;
import com.playlist.plitter.recommendations.application.dto.RecommendationResponse;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import com.playlist.plitter.track.domain.entity.TrackEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final RecommendationsRepository recommendationsRepository;

    public PlaylistCreateResponse savePlaylist(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

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

        List<RecommendationResponse> recommendations = recommendationsRepository.findAllByPlaylist(playlist)
                .stream()
                .map(recommendation -> {
                    TrackEntity track = recommendation.getTrack();
                    return new RecommendationResponse(
                            track.getSpotifyTrackId(),
                            track.getTitle(),
                            track.getArtistName(),
                            track.getAlbumCoverUrl(),
                            track.getPreviewUrl()
                    );
                })
                .toList();

        return new PlaylistResponse(
                playlist.getId(),
                recommendations.size(),
                recommendations.size() >= 30,
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

}
