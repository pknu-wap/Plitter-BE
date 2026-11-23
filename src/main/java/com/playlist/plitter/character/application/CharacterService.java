package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.CharacterConcept;
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
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int REQUIRED_RECOMMENDATION_COUNT = 5;
    private static final int SAVE_RETRY_COUNT = 2;

    private final CharacterRepository characterRepository;
    private final PlaylistRepository playlistRepository;
    private final MusicFeatureClient musicFeatureClient;
    private final CharacterEditSpecGenerator characterEditSpecGenerator;
    private final CharacterResultMetadataGenerator characterResultMetadataGenerator;
    private final LegacyCharacterResultMetadataGenerator legacyMetadataGenerator;
    private final MusicProfileAnalyzer musicProfileAnalyzer;
    private final CharacterConceptGenerator characterConceptGenerator;
    private final LegacyCharacterEditSpecGenerator legacyEditSpecGenerator;

    @Value("${character.generation.v2-enabled:true}")
    private boolean v2Enabled = true;
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
        if (!v2Enabled) featureSummaryJson = legacySummary(featureSummaryJson);
        List<CharacterEntity> recentCharacters = v2Enabled
                ? characterRepository.findTop3ByPlaylist_IdOrderByVersionDesc(playlistId) : List.of();
        CharacterConcept concept = v2Enabled ? characterConceptGenerator.generate(
                musicProfileAnalyzer.analyze(featureSummaryJson), UUID.randomUUID().getLeastSignificantBits(),
                getRecentConcepts(recentCharacters), getRecentMetadata(recentCharacters)) : null;
        CharacterEditSpec editSpec = concept == null ? legacyEditSpecGenerator.generate(featureSummaryJson) : generateEditSpec(concept);
        CharacterResultMetadata metadata = concept == null ? legacyMetadataGenerator.generate(featureSummaryJson)
                : characterResultMetadataGenerator.generate(concept);
        String generationSpecJson = concept == null ? null : serialize(concept);
        String resultMetadataJson = serialize(metadata);
        String editedCharacterImageUrl = editCharacterImage(editSpec);
        String storedCharacterImageUrl = storeCharacterImage(playlistId, editedCharacterImageUrl);

        CharacterEntity savedCharacter = saveCharacterWithTransaction(
                playlistId,
                requesterUserId,
                storedCharacterImageUrl,
                editSpec.promptText(),
                featureSummaryJson,
                generationSpecJson,
                resultMetadataJson
        );
        return new CharacterCreateResponse(
                savedCharacter.getId(),
                savedCharacter.getImageUrl(),
                metadata.characterName(),
                metadata.tags()
        );
    }

    public CharacterDetailResponse getCharacter(Long playlistId, Long requesterUserId) {
        CharacterEntity character = getLatestCreatedCharacterOrThrow(playlistId, requesterUserId);
        CharacterResultMetadata metadata = generateResultMetadata(character);
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
        CharacterResultMetadata metadata = generateResultMetadata(character);
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

    private CharacterEditSpec generateEditSpec(CharacterConcept concept) {
        try {
            CharacterEditSpec editSpec = characterEditSpecGenerator.generate(concept);
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
            String featureSummaryJson,
            String generationSpecJson,
            String resultMetadataJson
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
                                    featureSummaryJson,
                                    generationSpecJson,
                                    resultMetadataJson
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

    private CharacterResultMetadata generateResultMetadata(CharacterEntity character) {
        if (StringUtils.hasText(character.getResultMetadataJson())) {
            try {
                CharacterResultMetadata metadata = mapper.readValue(character.getResultMetadataJson(), CharacterResultMetadata.class);
                if (!StringUtils.hasText(metadata.characterName()) || metadata.tags() == null
                        || metadata.tags().size() != 3 || metadata.tags().stream().distinct().count() != 3
                        || metadata.tags().stream().anyMatch(t -> !StringUtils.hasText(t)))
                    throw new IllegalArgumentException("Invalid stored character metadata");
                return metadata;
            } catch (Exception e) {
                log.error("Invalid stored character metadata: characterId={}", character.getId(), e);
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }
        }
        if (StringUtils.hasText(character.getGenerationSpecJson())) {
            log.error("V2 character is missing stored metadata: characterId={}", character.getId());
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        return legacyMetadataGenerator.generate(character.getFeatureSummaryJson());
    }

    private List<CharacterResultMetadata> getRecentMetadata(List<CharacterEntity> recentCharacters) {
        List<CharacterResultMetadata> recent = new ArrayList<>();
        for (CharacterEntity character : recentCharacters) {
            try {
                recent.add(generateResultMetadata(character));
            } catch (ApiException e) {
                log.warn("Skipping invalid metadata history: characterId={}", character.getId());
            }
        }
        return recent;
    }

    private List<CharacterConcept> getRecentConcepts(List<CharacterEntity> recentCharacters) {
        List<CharacterConcept> recent = new ArrayList<>();
        for (CharacterEntity character : recentCharacters) {
            if (!StringUtils.hasText(character.getGenerationSpecJson())) continue;
            try {
                CharacterConcept concept = mapper.readValue(character.getGenerationSpecJson(), CharacterConcept.class);
                if (concept.schemaVersion() != 2 || concept.profile() == null || concept.profile().mood() == null
                        || !"v2".equals(concept.catalogVersion())
                        || concept.nameId() == null || concept.expressionId() == null || concept.poseId() == null
                        || concept.propId() == null || concept.marksId() == null)
                    throw new IllegalArgumentException("Invalid generation specification");
                characterResultMetadataGenerator.generate(concept);
                recent.add(concept);
            } catch (Exception e) {
                log.warn("Skipping invalid concept history: characterId={}", character.getId(), e);
            }
        }
        return recent;
    }

    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String legacySummary(String summary) {
        try {
            var root = mapper.readTree(summary);
            var profile = musicProfileAnalyzer.analyze(summary);
            String genre = switch (profile.genre()) {
                case "K_POP" -> "k-pop";
                case "HIP_HOP" -> "hip-hop";
                case "ELECTRONIC_DANCE" -> "dance";
                default -> profile.genre().toLowerCase(java.util.Locale.ROOT);
            };
            ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("primaryGenre", genre);
            return serialize(root);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }
}
