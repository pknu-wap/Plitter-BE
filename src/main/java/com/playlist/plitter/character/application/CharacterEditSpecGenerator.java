package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CharacterEditSpecGenerator {

    private static final double HIGH_ENERGY_THRESHOLD = 0.65;
    private static final double MID_ENERGY_THRESHOLD = 0.50;
    private static final double HIGH_VALENCE_THRESHOLD = 0.60;
    private static final double LOW_VALENCE_THRESHOLD = 0.40;
    private static final int MIN_CONFIDENT_FEATURE_COUNT = 2;
    private static final ConcurrentHashMap<String, AtomicInteger> VARIATION_COUNTER = new ConcurrentHashMap<>();

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
            String styleDirection = createStyleDirection(styleTone, genreHint);
            String variationDirective = createVariationDirective(styleTone, genreHint);
            String promptText = String.format(
                    """
                    Create a themed doodle variation based on the input star mascot.
                    The result should clearly feel like one new applied variation from the same doodle star character family.
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
                    Variation directive for this generation:
                    %s
                    Priority order:
                    1. Keep it recognizable as the same doodle star mascot family.
                    2. Keep the rough naive doodle style.
                    3. Apply a clearly visible music-inspired themed variation (pose + prop/accessory + expression).
                    4. Keep props and accessories simple and secondary.
                    """,
                    genreHint,
                    styleTone,
                    styleDirection,
                    variationDirective
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

    private String createStyleDirection(String styleTone, String genreHint) {
        return switch (styleTone) {
            case "energetic-bright" ->
                    "Push toward playful, bright, celebratory energy inspired by " + genreHint + ". " +
                            "Let the pose feel lively and extroverted. Choose expression, prop, and tiny mood marks freely within the doodle family.";
            case "energetic-intense" ->
                    "Push toward bold, kinetic, high-tension energy inspired by " + genreHint + ". " +
                            "Let the pose feel driven and assertive. Choose one strong visual hook without falling back to the same repeated accessory formula.";
            case "calm-deep" ->
                    "Push toward quiet, immersed, reflective energy inspired by " + genreHint + ". " +
                            "Let the pose feel relaxed or flowing. Choose one restrained prop or accessory and keep the mood subtle, spacious, and intimate.";
            default ->
                    "Push toward casual, friendly, everyday music energy inspired by " + genreHint + ". " +
                            "Let the pose feel natural and charming. Choose one light prop or accessory and avoid repeating a standard composition.";
        };
    }

    private String createVariationDirective(String styleTone, String genreHint) {
        List<String> variations = switch (styleTone) {
            case "energetic-bright" -> List.of(
                    "Create a busking-style variation with a cheerful face, a side-step pose, and one tiny performance prop inspired by " + genreHint + ".",
                    "Create a festival-style variation with a bouncing pose, one playful accessory, and two small sparkle or rhythm marks.",
                    "Create a dance-practice variation with a stretched pose, a mischievous smile, and one compact prop or badge.",
                    "Create a party-host variation with a welcoming pose, one celebratory handheld prop, and a more animated expression."
            );
            case "energetic-intense" -> List.of(
                    "Create a rock-stage variation with a sharp pose, one edgy prop inspired by " + genreHint + ", and short impact marks.",
                    "Create a sprinting variation with a forward-driving pose, a fierce or focused face, and one minimal performance accessory.",
                    "Create a DJ-or-hype variation with a punchy silhouette, one compact music tool, and strong rhythm marks.",
                    "Create a rebellious variation with asymmetrical motion, one standout accessory, and a more daring facial expression."
            );
            case "calm-deep" -> List.of(
                    "Create a late-night listening variation with a settled pose, a soft calm face, and one quiet music prop inspired by " + genreHint + ".",
                    "Create a wistful walking variation with a drifting pose, one restrained accessory, and one or two floating mood marks.",
                    "Create a jazz-club variation with a gentle sway, one compact instrument-like prop, and sparse note marks.",
                    "Create a dreamy pause variation with a still pose, a reflective face, and one subtle atmospheric prop or line detail."
            );
            default -> List.of(
                    "Create a casual humming variation with a light walking pose, one simple music prop, and a friendly face.",
                    "Create a songwriting variation with a small prop inspired by " + genreHint + " and a thoughtful but playful expression.",
                    "Create a weekend-groove variation with a relaxed pose, one charming accessory, and two tiny mood marks.",
                    "Create a street-doodle variation with a slightly awkward pose, one distinct prop, and a simple upbeat face."
            );
        };
        AtomicInteger counter = VARIATION_COUNTER.computeIfAbsent(styleTone, ignored -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), variations.size());
        return variations.get(index);
    }
}
