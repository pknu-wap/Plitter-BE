package com.playlist.plitter.character.application;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.character.application.port.BaseCharacterClient;
import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.MusicFeatureClient;
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

        String featureSummaryJson = collectFeatureSummary(playlistId);
        CharacterEditSpec editSpec = generateEditSpec(featureSummaryJson);
        String editedCharacterImageUrl = editCharacterImage(editSpec);
        String storedCharacterImageUrl = storeCharacterImage(playlistId, editedCharacterImageUrl);

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

    private CharacterCreateResponse saveCharacterWithTransaction(
            Long playlistId,
            String storedCharacterImageUrl,
            String promptText,
            String featureSummaryJson
    ) {
        for (int attempt = 1; attempt <= SAVE_RETRY_COUNT; attempt++) {
            try {
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
            } catch (DataIntegrityViolationException e) {
                if (attempt == SAVE_RETRY_COUNT) {
                    throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
                }
            }
        }
        throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
    }
}
