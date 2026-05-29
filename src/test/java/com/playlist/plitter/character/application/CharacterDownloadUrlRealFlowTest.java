package com.playlist.plitter.character.application;

import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CHARACTER_DOWNLOAD_REAL_TEST", matches = "true")
class CharacterDownloadUrlRealFlowTest {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getDownloadUrl_realFlow() throws Exception {
        String rawPlaylistId = System.getenv().getOrDefault("CHARACTER_REAL_FLOW_PLAYLIST_ID", "2");
        Long playlistId = Long.parseLong(rawPlaylistId);
        Long requesterUserId = findOwnerUserId(playlistId);

        CharacterDownloadUrlResponse response = characterService.getCharacterDownloadUrl(playlistId, requesterUserId);
        assertThat(response.downloadUrl()).isNotBlank();
        assertThat(response.expiresAt()).isNotNull();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(response.downloadUrl()))
                .GET()
                .build();
        HttpResponse<Void> httpResponse = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(httpResponse.statusCode()).isBetween(200, 299);

        System.out.println("CHARACTER_DOWNLOAD_RESULT playlistId=" + playlistId
                + " status=" + httpResponse.statusCode()
                + " expiresAt=" + response.expiresAt()
                + " downloadUrl=" + response.downloadUrl());
    }

    private Long findOwnerUserId(Long playlistId) {
        return entityManager.createQuery(
                        "select p.owner.id from PlaylistEntity p where p.id = :playlistId",
                        Long.class
                )
                .setParameter("playlistId", playlistId)
                .getSingleResult();
    }
}
