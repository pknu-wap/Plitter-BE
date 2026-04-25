package com.playlist.plitter.character.application;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private static final int REQUIRED_RECOMMENDATION_COUNT = 10;

    private final CharacterRepository characterRepository;
    private final PlaylistRepository playlistRepository;

    public CharacterAvailabilityResponse getAvailability(Long playlistId) {
        PlaylistEntity playlist = getPlaylistOrThrow(playlistId);
        int currentCount = playlist.getRecommendationCount();
        int missingCount = Math.max(REQUIRED_RECOMMENDATION_COUNT - currentCount, 0);

        return new CharacterAvailabilityResponse(
                missingCount == 0,
                REQUIRED_RECOMMENDATION_COUNT,
                currentCount,
                missingCount
        );
    }

    @Transactional
    public CharacterCreateResponse createCharacter(Long playlistId) {
        PlaylistEntity playlist = getPlaylistOrThrow(playlistId);
        CharacterAvailabilityResponse availability = getAvailability(playlistId);

        if (!availability.available()) {
            throw new IllegalStateException("Not enough recommendations to create character");
        }

        int nextVersion = characterRepository.findTopByPlaylist_IdOrderByVersionDesc(playlistId)
                .map(character -> character.getVersion() + 1)
                .orElse(1);

        String featureSummaryJson = buildFeatureSummaryJson(playlist);
        String promptText = buildPromptText(featureSummaryJson);
        String imageUrl = "https://cdn.plitter.local/characters/" + playlistId + "/v" + nextVersion + ".png";

        CharacterEntity savedCharacter = characterRepository.save(
                CharacterEntity.create(playlist, nextVersion, imageUrl, promptText, featureSummaryJson)
        );

        return new CharacterCreateResponse(savedCharacter.getId(), savedCharacter.getImageUrl());
    }

    public CharacterDetailResponse getCharacter(Long playlistId) {
        CharacterEntity character = getLatestCharacterOrThrow(playlistId);
        return new CharacterDetailResponse(character.getId(), character.getImageUrl());
    }

    public CharacterDownloadUrlResponse getCharacterDownloadUrl(Long playlistId) {
        CharacterEntity character = getLatestCharacterOrThrow(playlistId);
        String imageUrl = character.getImageUrl();
        return new CharacterDownloadUrlResponse(
                imageUrl + "?download=true",
                imageUrl,
                LocalDateTime.now().plusMinutes(30)
        );
    }

    private PlaylistEntity getPlaylistOrThrow(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
    }

    private CharacterEntity getLatestCharacterOrThrow(Long playlistId) {
        getPlaylistOrThrow(playlistId);
        return characterRepository.findTopByPlaylist_IdOrderByVersionDesc(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found for playlist: " + playlistId));
    }

    private String buildFeatureSummaryJson(PlaylistEntity playlist) {
        return String.format(
                "{\"playlistId\":%d,\"recommendationCount\":%d}",
                playlist.getId(),
                playlist.getRecommendationCount()
        );
    }

    private String buildPromptText(String featureSummaryJson) {
        return "Analyze music taste from summary: " + featureSummaryJson;
    }
}
