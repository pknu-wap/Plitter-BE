package com.playlist.plitter.character.application;

import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CHARACTER_REAL_FLOW_TEST", matches = "true")
class CharacterGenerationRealFlowTest {

    @Autowired
    private CharacterService characterService;

    @Test
    void createCharacter_realFlow() {
        String rawPlaylistId = System.getenv().getOrDefault("CHARACTER_REAL_FLOW_PLAYLIST_ID", "2");
        Long playlistId = Long.parseLong(rawPlaylistId);

        CharacterCreateResponse response = characterService.createCharacter(playlistId);

        assertThat(response.characterId()).isNotNull();
        assertThat(response.imageUrl()).isNotBlank();
        System.out.println("CHARACTER_REAL_FLOW_RESULT playlistId=" + playlistId
                + " characterId=" + response.characterId()
                + " imageUrl=" + response.imageUrl());
    }
}
