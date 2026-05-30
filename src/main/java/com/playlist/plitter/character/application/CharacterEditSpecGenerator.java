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
    private static final double LOW_ENERGY_THRESHOLD = 0.45;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterEditSpec generate(String featureSummaryJson) {
        try {
            JsonNode root = objectMapper.readTree(featureSummaryJson);
            double avgEnergy = root.path("avgEnergy").asDouble(0.0);
            double avgValence = root.path("avgValence").asDouble(0.0);
            String genreHint = toGenreHint(root.path("primaryGenre").asText("balanced"));

            String styleTone = createStyleTone(avgEnergy, avgValence);
            String styleDirection = createStyleDirection(styleTone, genreHint);
            String promptText = String.format(
                    """
                    The base character is a simple hand-drawn doodle star mascot with a five-point star body, thin stick-like arms and legs, tiny hands and feet, and a minimal face.
                    Edit the existing mascot while preserving its original silhouette, pose, proportions, and line style as closely as possible.
                    Adjust the character to reflect the given music mood:
                    - Genre inspiration: %s
                    - Style tone: %s
                    Character preservation rules:
                    - Keep the exact five-point star silhouette and body proportions from the input image.
                    - Keep limb count, limb placement, and full-body composition exactly the same as input.
                    - Keep the exact drawing style of the input image (line weight, stroke texture, and rendering style).
                    - Keep exactly one character; do not change character type or species.
                    - Accessories must not cover or obscure any star tip.
                    - Keep all accessories smaller than one star-tip length.
                    Allowed edits:
                    - Modify facial expression only.
                    - Add one or two compact accessories attached to the character.
                    - Add two or three tiny mood marks around the character (each shorter than one star tip length).
                    Rendering style rules:
                    - Line style must remain monochrome thin clean ink: consistent 0.3mm fineliner-like strokes, sharp edges, no fuzzy texture, no opacity variation, and no marker/brush/pencil effect.
                    - Slight wobble in line direction is allowed, but keep the stroke thin and consistent.
                    - No gradients, no 3D rendering, and no paper texture.
                    - Transparent background.
                    - Avoid solid filled areas. If an accessory needs interior detail, use sparse simple line hatching only.
                    Style direction:
                    %s
                    Important:
                    - Use the genre only as a light visual inspiration, not as a reason to redesign the character.
                    - If any style request conflicts with preserving the base character shape, preserving the base character shape always wins.
                    """,
                    genreHint,
                    styleTone,
                    styleDirection
            );
            return new CharacterEditSpec(promptText, styleTone);
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
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

    private String createStyleDirection(String styleTone, String genreHint) {
        return switch (styleTone) {
            case "energetic-bright" ->
                    "Use a bright lively expression with one small sporty accessory near the upper area, attached lightly without covering any star tip, and up to three tiny upbeat marks.";
            case "energetic-intense" ->
                    "Use a focused intense expression inspired by " + genreHint + ", choose one compact accessory that fits the mood, and add at most three short sharp motion lines.";
            case "calm-deep" ->
                    "Use a calm introspective expression inspired by " + genreHint + ", choose one small accessory that fits the mood, and add up to two soft floating marks.";
            default ->
                    "Use a relaxed natural expression inspired by " + genreHint + ", optionally add one tiny accessory, and add up to two subtle mood marks.";
        };
    }
}
