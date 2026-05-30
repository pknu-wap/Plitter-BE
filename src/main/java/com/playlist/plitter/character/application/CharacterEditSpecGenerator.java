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
            String styleDirection = createStyleDirection(styleTone);
            String promptText = String.format(
                    """
                    The base character is a simple hand-drawn doodle star mascot with a five-point star body, thin stick-like arms and legs, tiny hands and feet, and a minimal face.
                    Modify this same mascot from the input image; do not redraw or regenerate a new character.
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
                    - The stroke must look like a single clean ink line drawn with a 0.3mm fineliner pen.
                    - Each stroke must have sharp clean edges with no fuzziness, no texture, and no variation in opacity.
                    - Keep line thickness consistently thin and sharp.
                    - Do not use pencil, crayon, charcoal, chalk, marker, brush, or any soft/fuzzy/textured stroke style.
                    - Slight wobble in line direction is allowed, but keep stroke thickness thin and consistent.
                    - Keep the star silhouette slightly imperfect and asymmetric, not geometrically precise.
                    - Keep the overall drawing like a quick pen doodle: light and fast, not heavy or labored.
                    - Every line in body, accessories, and details must follow the same thin ink stroke style.
                    - Never use solid flat fill.
                    - If filling an accessory area, use at most 3 to 5 loosely spaced parallel lines.
                    - Hatching lines must be sparse and light, not dense or dark.
                    - Leave visible white space between hatching lines.
                    - Never use shaded texture or dense fill patterns.
                    - No smooth shading, no gradients, and no 3D rendering.
                    - Monochrome only: thin dark ink lines on plain white background.
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
        if (avgValence <= LOW_VALENCE_THRESHOLD) {
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
                    "Set expression to a wide smile with open bright eyes; add one small sporty accessory on the top star tip and up to three tiny sparkle or bounce lines.";
            case "energetic-intense" ->
                    "Set expression to focused eyes with slightly lowered brows; add one compact accessory such as tiny sunglasses or a thin headband and at most three short sharp motion lines.";
            case "calm-deep" ->
                    "Set expression to half-closed eyes with a small neutral mouth; add one small accessory such as tiny headphones or a thin scarf and up to two soft floating marks.";
            default ->
                    "Set expression to a relaxed friendly face; optionally add one tiny everyday accessory and up to two subtle mood marks.";
        };
    }
}
