package com.playlist.plitter.character.application;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.character.application.port.BaseCharacterClient;
import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.MusicFeatureClient;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;
import com.playlist.plitter.character.application.port.dto.DownloadUrlResult;
import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private static final int REQUIRED_RECOMMENDATION_COUNT = 10;
    private static final int SAVE_RETRY_COUNT = 2;

    private final CharacterRepository characterRepository;
    private final PlaylistRepository playlistRepository;
    private final MusicFeatureClient musicFeatureClient;
    private final CharacterEditSpecGenerator characterEditSpecGenerator;
    private final CharacterResultMetadataGenerator characterResultMetadataGenerator;
    private final BaseCharacterClient baseCharacterClient;
    private final CharacterImageEditClient characterImageEditClient;
    private final ImageStorageClient imageStorageClient;
    private final PlatformTransactionManager transactionManager;
    private final RecommendationsRepository recommendationsRepository;

    public CharacterAvailabilityResponse getAvailability(Long playlistId, Long requesterUserId) {
        PlaylistEntity playlist = getOwnedPlaylistOrThrow(playlistId, requesterUserId);
        int currentCount = getActualRecommendationCount(playlist);
        int missingCount = Math.max(REQUIRED_RECOMMENDATION_COUNT - currentCount, 0);

        return new CharacterAvailabilityResponse(
                isCreatable(currentCount),
                REQUIRED_RECOMMENDATION_COUNT,
                currentCount,
                missingCount
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CharacterCreateResponse createCharacter(Long playlistId, Long requesterUserId) {
        PlaylistEntity playlist = getOwnedPlaylistOrThrow(playlistId, requesterUserId);
        int currentCount = getActualRecommendationCount(playlist);
        if (!isCreatable(currentCount)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_NOT_AVAILABLE);
        }

        String featureSummaryJson = collectFeatureSummary(playlistId);
        CharacterEditSpec editSpec = generateEditSpec(featureSummaryJson);
        String editedCharacterImageUrl = editCharacterImage(editSpec);
        String storedCharacterImageUrl = storeCharacterImage(playlistId, editedCharacterImageUrl);

        CharacterEntity savedCharacter = saveCharacterWithTransaction(
                playlistId,
                requesterUserId,
                storedCharacterImageUrl,
                editSpec.promptText(),
                featureSummaryJson
        );
        CharacterResultMetadata metadata = generateResultMetadata(savedCharacter.getFeatureSummaryJson());
        return new CharacterCreateResponse(
                savedCharacter.getId(),
                savedCharacter.getImageUrl(),
                metadata.characterName(),
                metadata.tags()
        );
    }

    public CharacterDetailResponse getCharacter(Long playlistId, Long requesterUserId) {
        CharacterEntity character = getLatestCreatedCharacterOrThrow(playlistId, requesterUserId);
        CharacterResultMetadata metadata = generateResultMetadata(character.getFeatureSummaryJson());
        return new CharacterDetailResponse(
                character.getId(),
                character.getImageUrl(),
                metadata.characterName(),
                metadata.tags()
        );
    }

    public CharacterDownloadUrlResponse getCharacterDownloadUrl(Long playlistId, Long requesterUserId) {
        CharacterEntity character = getLatestCreatedCharacterOrThrow(playlistId, requesterUserId);
        DownloadUrlResult downloadUrlResult = imageStorageClient.createDownloadUrl(character.getImageUrl());
        CharacterResultMetadata metadata = generateResultMetadata(character.getFeatureSummaryJson());
        return new CharacterDownloadUrlResponse(
                downloadUrlResult.downloadUrl(),
                character.getImageUrl(),
                downloadUrlResult.expiresAt(),
                metadata.characterName(),
                metadata.tags()
        );
    }

    private PlaylistEntity getOwnedPlaylistOrThrow(Long playlistId, Long requesterUserId) {
        return playlistRepository.findByIdAndOwner_Id(playlistId, requesterUserId)
                .orElseThrow(() -> new ApiException(CharacterErrorCode.PLAYLIST_NOT_FOUND));
    }

    private CharacterEntity getLatestCreatedCharacterOrThrow(Long playlistId, Long requesterUserId) {
        getOwnedPlaylistOrThrow(playlistId, requesterUserId);
        return characterRepository.findTopByPlaylist_IdOrderByCreatedAtDescIdDesc(playlistId)
                .orElseThrow(() -> new ApiException(CharacterErrorCode.CHARACTER_NOT_FOUND));
    }

    private int getActualRecommendationCount(PlaylistEntity playlist) {
        return (int) recommendationsRepository.countByPlaylist(playlist);
    }

    private boolean isCreatable(int recommendationCount) {
        return recommendationCount >= REQUIRED_RECOMMENDATION_COUNT;
    }

    private String collectFeatureSummary(Long playlistId) {
        try {
            String featureSummaryJson = musicFeatureClient.collectFeatures(playlistId);
            return requireText(featureSummaryJson);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private CharacterEditSpec generateEditSpec(String featureSummaryJson) {
        try {
            CharacterEditSpec editSpec = characterEditSpecGenerator.generate(featureSummaryJson);
            requireText(editSpec.promptText());
            requireText(editSpec.styleTone());
            return editSpec;
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String editCharacterImage(CharacterEditSpec editSpec) {
        try {
            String imageUrl = characterImageEditClient.editCharacterImage(
                    new CharacterImageEditRequest(
                            baseCharacterClient.getBaseCharacter(),
                            editSpec
                    )
            );
            return requireText(imageUrl);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String storeCharacterImage(Long playlistId, String editedCharacterImageUrl) {
        try {
            String storedImageUrl = imageStorageClient.storeCharacterImage(playlistId, editedCharacterImageUrl);
            return requireText(storedImageUrl);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        return value;
    }

    private CharacterEntity saveCharacterWithTransaction(
            Long playlistId,
            Long requesterUserId,
            String storedCharacterImageUrl,
            String promptText,
            String featureSummaryJson
    ) {
        for (int attempt = 1; attempt <= SAVE_RETRY_COUNT; attempt++) {
            try {
                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                CharacterEntity response = transactionTemplate.execute(status -> {
                    PlaylistEntity managedPlaylist = getOwnedPlaylistOrThrow(playlistId, requesterUserId);
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
                    return savedCharacter;
                });
                return Objects.requireNonNull(response);
            } catch (DataIntegrityViolationException e) {
                if (attempt == SAVE_RETRY_COUNT) {
                    throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
                }
            }
        }
        throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
    }

    private CharacterResultMetadata generateResultMetadata(String featureSummaryJson) {
        return characterResultMetadataGenerator.generate(featureSummaryJson);
    }
}
