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
    private static final double MID_ENERGY_THRESHOLD = 0.50;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;
    private static final int MIN_CONFIDENT_FEATURE_COUNT = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterEditSpec generate(String featureSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(featureSummaryJson);
            double avgBpm = root.path("avgBpm").asDouble(0.0);
            double avgEnergy = root.path("avgEnergy").asDouble(0.0);
            double avgValence = root.path("avgValence").asDouble(0.0);
            int bpmCount = root.path("bpmCount").asInt(0);
            int energyCount = root.path("energyCount").asInt(0);
            int valenceCount = root.path("valenceCount").asInt(0);
            String genreHint = toGenreHint(root.path("primaryGenre").asText("balanced"));

            String styleTone = createStyleTone(avgBpm, avgEnergy, avgValence, bpmCount, energyCount, valenceCount);
            String promptText = String.format(
                    """
                    Keep the same base doodle star mascot silhouette and body proportions.
                    Generate exactly one full-body character in monochrome black/gray line-art.
                    Apply only small expression/accessory tweaks; do not add large props.
                    Do not redesign into another creature or human-like character.
                    Genre hint (light inspiration only): %s.
                    Style tone: %s.
                    Feature hints: avgBpm %.1f, avgEnergy %.2f, avgValence %.2f.
                    """,
                    genreHint,
                    styleTone,
                    avgBpm,
                    avgEnergy,
                    avgValence
            );
            return new CharacterEditSpec(promptText, styleTone);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String createStyleTone(
            double avgBpm,
            double avgEnergy,
            double avgValence,
            int bpmCount,
            int energyCount,
            int valenceCount
    ) {
        boolean hasReliableEnergy = energyCount >= MIN_CONFIDENT_FEATURE_COUNT;
        boolean hasReliableValence = valenceCount >= MIN_CONFIDENT_FEATURE_COUNT;
        boolean hasReliableBpm = bpmCount >= MIN_CONFIDENT_FEATURE_COUNT;

        if (!hasReliableEnergy && !hasReliableValence && !hasReliableBpm) {
            return "balanced";
        }

        if ((hasReliableEnergy && avgEnergy >= HIGH_ENERGY_THRESHOLD)
                || (hasReliableBpm && avgBpm >= HIGH_BPM_THRESHOLD)) {
            if (!hasReliableValence) {
                return "balanced";
            }
            return avgValence >= HIGH_VALENCE_THRESHOLD ? "energetic-bright" : "energetic-intense";
        }

        if (hasReliableValence && avgValence <= LOW_VALENCE_THRESHOLD) {
            if (hasReliableEnergy && avgEnergy >= MID_ENERGY_THRESHOLD) {
                return "balanced";
            }
            return "calm-deep";
        }

        return "balanced";
    }

    private String toGenreHint(String primaryGenre) {
        String normalized = primaryGenre == null ? "" : primaryGenre.trim().toLowerCase();
        return switch (normalized) {
            case "k-pop", "kpop" -> "k-pop inspired";
            case "hip-hop", "hiphop", "rap" -> "hip-hop inspired";
            case "rock" -> "rock inspired";
            case "ballad" -> "ballad inspired";
            case "r&b", "rnb" -> "r&b inspired";
            case "edm", "dance" -> "electronic dance inspired";
            case "jazz" -> "jazz inspired";
            default -> "balanced contemporary";
        };
    }
}
