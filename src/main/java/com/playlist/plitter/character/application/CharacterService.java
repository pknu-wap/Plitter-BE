package com.playlist.plitter.character.application;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.character.application.port.BaseCharacterClient;
import com.playlist.plitter.character.application.port.CharacterAiClient;
import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.MusicFeatureClient;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpecRequest;
import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;
import com.playlist.plitter.character.application.port.dto.DownloadUrlResult;
import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private static final int REQUIRED_RECOMMENDATION_COUNT = 10;

    private final CharacterRepository characterRepository;
    private final PlaylistRepository playlistRepository;
    private final MusicFeatureClient musicFeatureClient;
    private final CharacterAiClient characterAiClient;
    private final BaseCharacterClient baseCharacterClient;
    private final CharacterImageEditClient characterImageEditClient;
    private final ImageStorageClient imageStorageClient;
    private final PlatformTransactionManager transactionManager;

    public CharacterAvailabilityResponse getAvailability(Long playlistId) {
        PlaylistEntity playlist = getPlaylistOrThrow(playlistId);
        int currentCount = playlist.getRecommendationCount();
        int missingCount = Math.max(REQUIRED_RECOMMENDATION_COUNT - currentCount, 0);

        return new CharacterAvailabilityResponse(
                isCreatable(playlist),
                REQUIRED_RECOMMENDATION_COUNT,
                currentCount,
                missingCount
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CharacterCreateResponse createCharacter(Long playlistId) {
        PlaylistEntity playlist = getPlaylistOrThrow(playlistId);
        if (!isCreatable(playlist)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_NOT_AVAILABLE);
        }

        String featureSummaryJson = musicFeatureClient.collectFeatures(playlistId);
        CharacterEditSpec editSpec = characterAiClient.createEditSpec(
                new CharacterEditSpecRequest(playlistId, featureSummaryJson)
        );
        String editedCharacterImageUrl = characterImageEditClient.editCharacterImage(
                new CharacterImageEditRequest(
                        baseCharacterClient.getBaseCharacter(),
                        editSpec
                )
        );
        String storedCharacterImageUrl = imageStorageClient.storeCharacterImage(
                playlistId,
                editedCharacterImageUrl
        );
        return saveCharacterWithTransaction(
                playlistId,
                storedCharacterImageUrl,
                editSpec.promptText(),
                featureSummaryJson
        );
    }

    public CharacterDetailResponse getCharacter(Long playlistId) {
        CharacterEntity character = getLatestCharacterOrThrow(playlistId);
        return new CharacterDetailResponse(character.getId(), character.getImageUrl());
    }

    public CharacterDownloadUrlResponse getCharacterDownloadUrl(Long playlistId) {
        CharacterEntity character = getLatestCharacterOrThrow(playlistId);
        DownloadUrlResult downloadUrlResult = imageStorageClient.createDownloadUrl(character.getImageUrl());
        return new CharacterDownloadUrlResponse(
                downloadUrlResult.downloadUrl(),
                character.getImageUrl(),
                downloadUrlResult.expiresAt()
        );
    }

    private PlaylistEntity getPlaylistOrThrow(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ApiException(CharacterErrorCode.PLAYLIST_NOT_FOUND));
    }

    private CharacterEntity getLatestCharacterOrThrow(Long playlistId) {
        getPlaylistOrThrow(playlistId);
        return characterRepository.findTopByPlaylist_IdOrderByVersionDesc(playlistId)
                .orElseThrow(() -> new ApiException(CharacterErrorCode.CHARACTER_NOT_FOUND));
    }

    private boolean isCreatable(PlaylistEntity playlist) {
        return playlist.getRecommendationCount() >= REQUIRED_RECOMMENDATION_COUNT;
    }

    private CharacterCreateResponse saveCharacterWithTransaction(
            Long playlistId,
            String storedCharacterImageUrl,
            String promptText,
            String featureSummaryJson
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CharacterCreateResponse response = transactionTemplate.execute(status -> {
            PlaylistEntity managedPlaylist = getPlaylistOrThrow(playlistId);
            int nextVersion = characterRepository.findTopByPlaylist_IdOrderByVersionDesc(playlistId)
                    .map(character -> character.getVersion() + 1)
                    .orElse(1);

            CharacterEntity savedCharacter = characterRepository.save(
                    CharacterEntity.create(
                            managedPlaylist,
                            nextVersion,
                            storedCharacterImageUrl,
                            promptText,
                            featureSummaryJson
                    )
            );
            return new CharacterCreateResponse(savedCharacter.getId(), savedCharacter.getImageUrl());
        });
        return Objects.requireNonNull(response);
    }
}
