package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;
import com.playlist.plitter.character.domain.entity.CharacterEntity;
import com.playlist.plitter.character.domain.repository.CharacterRepository;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@EnabledIfEnvironmentVariable(named = "CHARACTER_SMOKE_TEST", matches = "true")
class CharacterGenerationSmokeTest {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createCharacter_generatesAndPersistsCharacter() {
        Long playlistId = findCreatablePlaylistId();
        Long requesterUserId = findOwnerUserId(playlistId);

        CharacterCreateResponse response = characterService.createCharacter(playlistId, requesterUserId);
        Long characterId = response.characterId();
        String expectedStoredImageUrl = "https://mock.local/stored/" + playlistId + ".png";

        try {
            assertThat(characterId).isNotNull();
            assertThat(response.imageUrl()).isEqualTo(expectedStoredImageUrl);

            Optional<CharacterEntity> saved = characterRepository.findById(characterId);
            assertThat(saved).isPresent();
            assertThat(saved.get().getImageUrl()).isEqualTo(expectedStoredImageUrl);
            assertThat(saved.get().getPromptText()).isNotBlank();
            assertThat(saved.get().getFeatureSummaryJson()).isNotBlank();
        } finally {
//            if (characterId != null) {
//                characterRepository.deleteById(characterId);
//            }
        }
    }

    private Long findCreatablePlaylistId() {
        return entityManager.createQuery(
                        "select p.id from PlaylistEntity p where p.recommendationCount >= :required order by p.id desc",
                        Long.class
                )
                .setParameter("required", 5)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("캐릭터 생성 가능한 플레이리스트가 없습니다."));
    }

    private Long findOwnerUserId(Long playlistId) {
        return entityManager.createQuery(
                        "select p.owner.id from PlaylistEntity p where p.id = :playlistId",
                        Long.class
                )
                .setParameter("playlistId", playlistId)
                .getSingleResult();
    }

    @TestConfiguration
    static class StubClientConfig {
        private final AtomicLong imageCounter = new AtomicLong(0L);

        @Bean("openAiCharacterImageEditClient")
        CharacterImageEditClient testCharacterImageEditClient() {
            return new CharacterImageEditClient() {
                @Override
                public String editCharacterImage(CharacterImageEditRequest request) {
                    return "https://mock.local/generated/" + imageCounter.incrementAndGet() + ".png";
                }
            };
        }

        @Bean("s3ImageStorageClient")
        ImageStorageClient testImageStorageClient() {
            return new ImageStorageClient() {
                @Override
                public String storeCharacterImage(Long playlistId, String sourceImageUrl) {
                    return "https://mock.local/stored/" + playlistId + ".png";
                }

                @Override
                public com.playlist.plitter.character.application.port.dto.DownloadUrlResult createDownloadUrl(String imageUrl) {
                    throw new UnsupportedOperationException("not used in this test");
                }
            };
        }
    }
}
