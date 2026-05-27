package com.playlist.plitter.track.infrastructure.lastfm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LastFmTagClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.builder().build();

    @Value("${lastfm.api-key:}")
    private String apiKey;

    @Value("${lastfm.base-url:https://ws.audioscrobbler.com/2.0/}")
    private String baseUrl;

    public LastFmTagBundle fetchTopTags(String title, String artistName) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(title) || !StringUtils.hasText(artistName)) {
            return new LastFmTagBundle(List.of(), List.of(), "{}");
        }

        JsonNode trackResponse = callLastFm("track.getTopTags", Map.of(
                "track", title,
                "artist", artistName,
                "autocorrect", "1"
        ));
        JsonNode artistResponse = callLastFm("artist.getTopTags", Map.of(
                "artist", artistName,
                "autocorrect", "1"
        ));

        List<LastFmTag> trackTags = extractTags(trackResponse);
        List<LastFmTag> artistTags = extractTags(artistResponse);

        return new LastFmTagBundle(trackTags, artistTags, toRawJson(trackResponse, artistResponse));
    }

    private JsonNode callLastFm(String method, Map<String, String> params) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("method", method)
                    .queryParam("api_key", apiKey)
                    .queryParam("format", "json");

            params.forEach(builder::queryParam);
            URI uri = builder.build().encode().toUri();

            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(body)) {
                return objectMapper.createObjectNode();
            }

            JsonNode root = objectMapper.readTree(body);
            if (root.has("error")) {
                log.warn("Last.fm API error: method={}, error={}, message={}",
                        method,
                        root.path("error").asInt(),
                        root.path("message").asText());
                return objectMapper.createObjectNode();
            }
            return root;
        } catch (Exception exception) {
            log.warn("Last.fm call failed: method={}", method, exception);
            return objectMapper.createObjectNode();
        }
    }

    private List<LastFmTag> extractTags(JsonNode root) {
        JsonNode tagNodes = root.path("toptags").path("tag");
        if (tagNodes.isMissingNode() || tagNodes.isNull()) {
            return List.of();
        }

        List<LastFmTag> result = new ArrayList<>();
        if (tagNodes.isArray()) {
            for (JsonNode tagNode : tagNodes) {
                addTag(result, tagNode);
            }
            return result;
        }

        addTag(result, tagNodes);
        return result;
    }

    private void addTag(List<LastFmTag> tags, JsonNode node) {
        String name = node.path("name").asText("").trim();
        if (!StringUtils.hasText(name)) {
            return;
        }
        int count = node.path("count").asInt(0);
        tags.add(new LastFmTag(name, count));
    }

    private String toRawJson(JsonNode trackResponse, JsonNode artistResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trackTopTags", trackResponse);
        payload.put("artistTopTags", artistResponse);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
