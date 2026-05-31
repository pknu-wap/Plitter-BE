package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CharacterResultMetadataGenerator {

    private static final double HIGH_ENERGY_THRESHOLD = 0.65;
    private static final double LOW_ENERGY_THRESHOLD = 0.45;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterResultMetadata generate(String featureSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(featureSummaryJson);
            double avgEnergy = root.path("avgEnergy").asDouble(0.0);
            double avgValence = root.path("avgValence").asDouble(0.0);
            String primaryGenre = root.path("primaryGenre").asText("balanced");
            String styleTone = createStyleTone(avgEnergy, avgValence);

            String characterName = buildCharacterName(primaryGenre, styleTone);
            List<String> tags = List.of(
                    "#" + styleToneTag(styleTone),
                    "#" + energyTag(avgEnergy),
                    "#" + valenceTag(avgValence)
            );
            return new CharacterResultMetadata(characterName, tags);
        } catch (Exception e) {
            return new CharacterResultMetadata("리듬 스타", List.of("#조화로운", "#감각적인", "#분위기있는"));
        }
    }

    private String createStyleTone(double avgEnergy, double avgValence) {
        if (avgEnergy >= HIGH_ENERGY_THRESHOLD) {
            return avgValence >= HIGH_VALENCE_THRESHOLD ? "energetic-bright" : "energetic-intense";
        }
        if (avgEnergy <= LOW_ENERGY_THRESHOLD && avgValence <= LOW_VALENCE_THRESHOLD) {
            return "calm-deep";
        }
        return "balanced";
    }

    private String buildCharacterName(String primaryGenre, String styleTone) {
        String genreLabel = toGenreLabel(primaryGenre);
        String toneLabel = switch (styleTone) {
            case "energetic-bright" -> "스텝";
            case "energetic-intense" -> "비트";
            case "calm-deep" -> "무드";
            default -> "리듬";
        };
        return genreLabel + " " + toneLabel + " 스타";
    }

    private String toGenreLabel(String genre) {
        if (!StringUtils.hasText(genre)) {
            return "플리";
        }
        String normalized = genre.trim().toLowerCase();
        return switch (normalized) {
            case "k-pop", "kpop" -> "케이팝";
            case "hip-hop", "hiphop", "rap" -> "힙합";
            case "rock" -> "록";
            case "ballad" -> "발라드";
            case "r&b", "rnb" -> "알앤비";
            case "edm", "dance" -> "댄스";
            case "jazz" -> "재즈";
            default -> "플리";
        };
    }

    private String styleToneTag(String styleTone) {
        return switch (styleTone) {
            case "energetic-bright" -> "밝은";
            case "energetic-intense" -> "강렬한";
            case "calm-deep" -> "잔잔한";
            default -> "조화로운";
        };
    }

    private String energyTag(double avgEnergy) {
        return avgEnergy >= HIGH_ENERGY_THRESHOLD ? "열정적인" : "차분한";
    }

    private String valenceTag(double avgValence) {
        if (avgValence >= HIGH_VALENCE_THRESHOLD) {
            return "쾌활한";
        }
        if (avgValence <= LOW_VALENCE_THRESHOLD) {
            return "감성적인";
        }
        return "균형잡힌";
    }
}
