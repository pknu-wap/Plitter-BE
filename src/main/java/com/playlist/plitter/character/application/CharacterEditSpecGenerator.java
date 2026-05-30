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
                    Create a themed doodle variation based on the input star mascot.
                    The result should clearly feel like an applied variation from the same doodle star character family.
                    Preserve the character identity, not the exact original pose or exact limb placement.
                    Music mood:
                    - Genre inspiration: %s
                    - Style tone: %s
                    Preserve the character identity:
                    - simple five-point star body
                    - rough naive hand-drawn doodle style
                    - thin stick-like arms and legs
                    - tiny simple hands and feet
                    - minimal cute face
                    - awkward uneven asymmetry
                    - simple full-body composition
                    Allowed themed variation:
                    - Change the facial expression to match the mood.
                    - Adjust arm and leg pose to create a clear music-themed situation.
                    - Add one music-themed prop or one accessory inspired by the genre.
                    - Add up to three tiny mood marks, music notes, sparkles, or motion marks around the character.
                    Prop and accessory limits:
                    - Use only one main prop or accessory.
                    - The prop must be visually secondary to the star character.
                    - The prop must be no larger than one half of the star body.
                    - Accessories attached to the body must be smaller than the face area.
                    - Do not cover the face.
                    - Do not hide or replace the star silhouette.
                    - Do not add complex clothing or a full outfit.
                    - Do not turn the character into another species, object, or human-like figure.
                    Rendering style rules:
                    - Keep it as a rough black-and-white doodle.
                    - Use thin, wobbly, uneven black lines similar to the input image.
                    - Do not polish, smooth, vectorize, thicken, or professionalize the line art.
                    - Preserve small irregularities and naive hand-drawn imperfections.
                    - No color, no shading, no gradients, no 3D rendering, and no paper texture.
                    - Transparent background.
                    - Avoid solid filled areas; use sparse simple hatching only if necessary.
                    Style direction:
                    %s
                    Priority order:
                    1. Keep it recognizable as the same doodle star mascot family.
                    2. Keep the rough naive doodle style.
                    3. Apply a clearly visible music-inspired themed variation (pose + prop/accessory + expression).
                    4. Keep props and accessories simple and secondary.
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
                    "Use a cheerful open expression. Create an active upbeat pose. Add tiny music notes, sparkle marks, or bounce marks. Use one prop/accessory such as a tiny microphone or cap, kept secondary.";
            case "energetic-intense" ->
                    "Use focused eyes with slightly lowered brows inspired by " + genreHint + ". Create a dynamic intense pose. Add short motion marks. Use one prop/accessory such as tiny sunglasses, a small microphone, or a small guitar-like doodle prop, kept secondary.";
            case "calm-deep" ->
                    "Use calm half-closed eyes with a tiny relaxed mouth inspired by " + genreHint + ". Create a relaxed flowing pose. Add one or two floating music notes. Use one prop/accessory such as tiny headphones, a small saxophone-like doodle prop, or a thin scarf-like line detail, kept secondary.";
            default ->
                    "Use a relaxed friendly expression inspired by " + genreHint + ". Create a simple playful pose. Add subtle music notes or mood marks. Use one very simple small prop inspired by the genre, kept secondary.";
        };
    }
}
