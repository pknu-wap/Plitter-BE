package com.playlist.plitter.character.infrastructure.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.MusicProfile;
import com.playlist.plitter.character.application.port.dto.CharacterMetadataRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenAiCharacterMetadataClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.invalid");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OpenAiCharacterMetadataClient client = new OpenAiCharacterMetadataClient(true, "fixture-key", "gpt-6-luna", builder);
    private final CharacterMetadataRequest request = new CharacterMetadataRequest(
            new MusicProfile(MusicProfile.Mood.LOW_LOW, "BALLAD", .25, .3, 5, 5, 5), "#발라드",
            "A gentle smile.", "Stand quietly.", "A tiny music player.", "Two music notes.", List.of(), "");

    @Test void usesOneBoundedStructuredRequestAndParsesMessageAfterReasoningItem() throws Exception {
        String generated = mapper.writeValueAsString(Map.of("characterName", "전주부터 사연 있는 스타",
                "tags", List.of("새벽감성", "혼자엔딩중")));
        String response = mapper.writeValueAsString(Map.of("status", "completed", "output", List.of(
                Map.of("type", "reasoning", "summary", List.of()),
                Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", generated)))),
                "usage", Map.of("input_tokens", 1000, "output_tokens", 80)));
        server.expect(requestTo("https://provider.invalid/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer fixture-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(httpRequest -> {
                    var body = mapper.readTree(((MockClientHttpRequest) httpRequest).getBodyAsString());
                    assertEquals("gpt-6-luna", body.path("model").asText());
                    assertFalse(body.path("store").asBoolean());
                    assertEquals(256, body.path("max_output_tokens").asInt());
                    assertEquals("none", body.path("reasoning").path("effort").asText());
                    var format = body.path("text").path("format");
                    assertEquals("json_schema", format.path("type").asText());
                    assertTrue(format.path("strict").asBoolean());
                    assertEquals(2, format.path("schema").path("properties").path("tags").path("maxItems").asInt());
                    var input = mapper.readTree(body.path("input").asText());
                    assertEquals("#발라드", input.path("genreTag").asText());
                    assertEquals(.25, input.path("musicProfile").path("energy").asDouble());
                    assertEquals("A tiny music player.", input.path("prop").asText());
                }).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var result = client.generate(request);

        assertEquals("전주부터 사연 있는 스타", result.characterName());
        assertEquals(List.of("새벽감성", "혼자엔딩중"), result.tags());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status\":\"incomplete\",\"output\":[]}",
            "{\"status\":\"completed\",\"output\":[]}",
            "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"No\"}]}]}",
            "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"broken-json\"}]}]}",
            "null"
    })
    void rejectsIncompleteRefusedAndMalformedResponses(String response) {
        server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, () -> client.generate(request));
        server.verify();
    }

    @Test void providerErrorsDoNotExposeResponseBodyOrKey() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .body("sensitive fixture-key provider body").contentType(MediaType.APPLICATION_JSON));
        var error = assertThrows(IllegalStateException.class, () -> client.generate(request));
        assertFalse(error.getMessage().contains("fixture-key"));
        assertFalse(error.getMessage().contains("sensitive"));
        assertNull(error.getCause());
        server.verify();
    }

    @Test void networkTimeoutPropagatesAsSafeFailureForApplicationFallback() {
        server.expect(anything()).andRespond(withException(new IOException("request timed out")));
        assertThrows(IllegalStateException.class, () -> client.generate(request));
        server.verify();
    }

    @Test void missingKeyAndDisabledFeatureSkipNetworkRequests() {
        var missingKey = new OpenAiCharacterMetadataClient(true, " ", "gpt-6-luna", RestClient.builder());
        var disabled = new OpenAiCharacterMetadataClient(false, "fixture-key", "gpt-6-luna", RestClient.builder());
        assertFalse(missingKey.isAvailable());
        assertFalse(disabled.isAvailable());
        assertThrows(IllegalStateException.class, () -> missingKey.generate(request));
        assertThrows(IllegalStateException.class, () -> disabled.generate(request));
        server.verify();
    }
}
