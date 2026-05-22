package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import org.springframework.stereotype.Component;

@Component
public class CharacterEditSpecGenerator {

    private static final double HIGH_BPM_THRESHOLD = 120.0;
    private static final double HIGH_ENERGY_THRESHOLD = 0.65;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterEditSpec generate(String featureSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(featureSummaryJson);
            double avgBpm = root.path("avgBpm").asDouble(0.0);
            double avgEnergy = root.path("avgEnergy").asDouble(0.0);
            double avgValence = root.path("avgValence").asDouble(0.0);
            String primaryGenre = root.path("primaryGenre").asText("balanced");

            String styleTone = createStyleTone(avgBpm, avgEnergy, avgValence);
            String promptText = String.format(
                    "Apply a %s visual mood based on %s genre, avgBpm %.1f, avgEnergy %.2f, avgValence %.2f.",
                    styleTone,
                    primaryGenre,
                    avgBpm,
                    avgEnergy,
                    avgValence
            );
            return new CharacterEditSpec(promptText, styleTone);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String createStyleTone(double avgBpm, double avgEnergy, double avgValence) {
        if (avgEnergy >= HIGH_ENERGY_THRESHOLD || avgBpm >= HIGH_BPM_THRESHOLD) {
            return avgValence >= HIGH_VALENCE_THRESHOLD ? "energetic-bright" : "energetic-intense";
        }
        if (avgValence <= LOW_VALENCE_THRESHOLD) {
            return "calm-deep";
        }
        return "balanced";
    }
}
