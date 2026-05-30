package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import org.springframework.stereotype.Component;

@Component
public class CharacterEditSpecGenerator {

    private static final double HIGH_ENERGY_THRESHOLD = 0.65;
    private static final double MID_ENERGY_THRESHOLD = 0.50;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;
    private static final int MIN_CONFIDENT_FEATURE_COUNT = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterEditSpec generate(String featureSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(featureSummaryJson);
            double avgEnergy = root.path("avgEnergy").asDouble(0.0);
            double avgValence = root.path("avgValence").asDouble(0.0);
            int energyCount = root.path("energyCount").asInt(0);
            int valenceCount = root.path("valenceCount").asInt(0);
            String genreHint = toGenreHint(root.path("primaryGenre").asText("balanced"));

            String styleTone = createStyleTone(avgEnergy, avgValence, energyCount, valenceCount);
            String styleDirection = createStyleDirection(styleTone);
            String promptText = String.format(
                    """
                    The base character is a simple hand-drawn doodle star mascot with a five-point star body, thin stick-like arms and legs, tiny hands and feet, and a minimal face.
                    Create a variation of this same mascot, not a new character.
                    Edit the character to reflect the given music mood:
                    - Genre inspiration: %s
                    - Style tone: %s
                    - avgEnergy: %.2f
                    - avgValence: %.2f
                    Character preservation rules:
                    - Preserve the same recognizable five-point star body silhouette.
                    - Preserve the thin stick-like arms and legs.
                    - Preserve the tiny simple hands and feet.
                    - Preserve the simple doodle mascot identity.
                    - Preserve the same full-body composition.
                    - Preserve the same limb count, limb placement, and body proportions.
                    - Keep the star outline fully readable; do not hide major star tips with large accessories.
                    - Accessories must not change the main star outline except for tiny attached details.
                    - Do not turn the character into a human, animal, monster, robot, or different creature.
                    - Do not add extra characters. Generate exactly one full-body character.
                    - Do not make the background the main change.
                    Allowed edits:
                    - Change the facial expression.
                    - Add small outfit details.
                    - Add compact accessories.
                    - Add subtle music-inspired visual details directly on or around the character.
                    Rendering style rules:
                    - Keep a rough hand-drawn doodle line-art look.
                    - Do not fill the character body with solid colors.
                    - Do not fill the background with colors.
                    - Use mostly black or dark gray stroke lines.
                    - Do not use accent colors; keep all edits monochrome line-art (black/gray only).
                    - Keep stroke thickness simple and fairly thin.
                    - Avoid gradients, painterly shading, or 3D rendering.
                    - Avoid heavy cross-hatching or dense texture rendering.
                    - Keep the background plain and simple.
                    Style direction:
                    %s
                    Important:
                    - Use the genre only as a light visual inspiration, not as a reason to redesign the character.
                    - If any style request conflicts with preserving the base character shape, preserving the base character shape always wins.
                    """,
                    genreHint,
                    styleTone,
                    avgEnergy,
                    avgValence,
                    styleDirection
            );
            return new CharacterEditSpec(promptText, styleTone);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private String createStyleTone(double avgEnergy, double avgValence, int energyCount, int valenceCount) {
        boolean hasReliableEnergy = energyCount >= MIN_CONFIDENT_FEATURE_COUNT;
        boolean hasReliableValence = valenceCount >= MIN_CONFIDENT_FEATURE_COUNT;

        if (!hasReliableEnergy && !hasReliableValence) {
            return "balanced";
        }

        if (hasReliableEnergy && avgEnergy >= HIGH_ENERGY_THRESHOLD) {
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

    private String createStyleDirection(String styleTone) {
        return switch (styleTone) {
            case "energetic-bright" ->
                    "Use a cheerful confident expression, upbeat motion lines, tiny sparkle marks, light sporty details, or small playful accessories. Keep all additions compact and doodle-like.";
            case "energetic-intense" ->
                    "Use an intense focused expression, small sunglasses, a tiny headband, sharp motion lines, or bold but minimal line-art accessories. Do not make the character aggressive or creature-like.";
            case "calm-deep" ->
                    "Use a calm thoughtful expression, tiny headphones, a small scarf, soft floating lines, or restrained music-note details. Keep the mood quiet and minimal.";
            default ->
                    "Use a natural friendly expression, simple everyday accessory details, and balanced doodle styling. Keep the character close to the base design.";
        };
    }
}
