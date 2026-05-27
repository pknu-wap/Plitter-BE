package com.playlist.plitter.track.application.feature;

import com.playlist.plitter.track.infrastructure.lastfm.LastFmTag;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MetadataTagFeatureEstimator {

    private static final Set<String> GENRE_TAGS = Set.of(
            "pop", "k-pop", "rock", "indie", "hip hop", "rap", "r&b", "jazz",
            "classical", "electronic", "dance", "metal", "ballad", "folk", "acoustic"
    );

    private static final Set<String> HIGH_ENERGY_TAGS = Set.of(
            "dance", "edm", "electronic", "house", "techno", "party", "workout", "metal", "punk"
    );

    private static final Set<String> LOW_ENERGY_TAGS = Set.of(
            "acoustic", "ambient", "chill", "lofi", "ballad", "piano", "sleep", "calm"
    );

    private static final Set<String> POSITIVE_VALENCE_TAGS = Set.of(
            "happy", "upbeat", "feel good", "summer", "dance", "party", "bright"
    );

    private static final Set<String> NEGATIVE_VALENCE_TAGS = Set.of(
            "sad", "melancholic", "dark", "emo", "depressing", "lonely"
    );

    private static final Set<String> SAD_MOOD_TAGS = Set.of("sad", "melancholic", "emo", "dark");
    private static final Set<String> CHILL_MOOD_TAGS = Set.of("chill", "ambient", "acoustic", "calm", "lofi");

    public EstimatedMetadataFeatures estimate(List<LastFmTag> trackTags, List<LastFmTag> artistTags) {
        Map<String, Double> weightedTags = new LinkedHashMap<>();
        mergeWeightedTags(weightedTags, trackTags, 1.0d);
        mergeWeightedTags(weightedTags, artistTags, 0.65d);

        if (weightedTags.isEmpty()) {
            return new EstimatedMetadataFeatures("balanced", "balanced", null, null, "LASTFM_EMPTY", BigDecimal.ZERO);
        }

        String genre = findPrimaryGenre(weightedTags);
        BigDecimal energy = estimateEnergy(weightedTags);
        BigDecimal valence = estimateValence(weightedTags);
        String mood = estimateMood(weightedTags, energy, valence);
        BigDecimal confidence = estimateConfidence(weightedTags, trackTags, artistTags);

        return new EstimatedMetadataFeatures(mood, genre, energy, valence, "LASTFM_TAG", confidence);
    }

    private void mergeWeightedTags(Map<String, Double> weightedTags, List<LastFmTag> tags, double sourceWeight) {
        for (LastFmTag tag : tags) {
            String normalized = normalize(tag.name());
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            double baseWeight = tag.count() > 0 ? Math.min(1.0d, tag.count() / 100.0d) : 0.35d;
            weightedTags.merge(normalized, baseWeight * sourceWeight, Double::sum);
        }
    }

    private String findPrimaryGenre(Map<String, Double> weightedTags) {
        String topGenre = null;
        double topWeight = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> entry : weightedTags.entrySet()) {
            if (GENRE_TAGS.contains(entry.getKey()) && entry.getValue() > topWeight) {
                topWeight = entry.getValue();
                topGenre = entry.getKey();
            }
        }

        if (topGenre != null) {
            return topGenre;
        }

        return weightedTags.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("balanced");
    }

    private BigDecimal estimateEnergy(Map<String, Double> weightedTags) {
        double high = sumWeights(weightedTags, HIGH_ENERGY_TAGS);
        double low = sumWeights(weightedTags, LOW_ENERGY_TAGS);
        double score = 0.5d + ((high - low) / Math.max(1.0d, high + low + 1.5d));
        return toTwoDecimal(clamp01(score));
    }

    private BigDecimal estimateValence(Map<String, Double> weightedTags) {
        double positive = sumWeights(weightedTags, POSITIVE_VALENCE_TAGS);
        double negative = sumWeights(weightedTags, NEGATIVE_VALENCE_TAGS);
        double score = 0.5d + ((positive - negative) / Math.max(1.0d, positive + negative + 1.5d));
        return toTwoDecimal(clamp01(score));
    }

    private String estimateMood(Map<String, Double> weightedTags, BigDecimal energy, BigDecimal valence) {
        if (containsAny(weightedTags, SAD_MOOD_TAGS) && valence != null && valence.doubleValue() < 0.45d) {
            return "melancholic";
        }
        if (containsAny(weightedTags, CHILL_MOOD_TAGS) && energy != null && energy.doubleValue() < 0.45d) {
            return "calm";
        }
        if (energy != null && valence != null && energy.doubleValue() >= 0.67d && valence.doubleValue() >= 0.55d) {
            return "uplifting";
        }
        if (energy != null && energy.doubleValue() >= 0.67d) {
            return "intense";
        }
        return "balanced";
    }

    private BigDecimal estimateConfidence(
            Map<String, Double> weightedTags,
            List<LastFmTag> trackTags,
            List<LastFmTag> artistTags
    ) {
        double total = weightedTags.values().stream().mapToDouble(Double::doubleValue).sum();
        double score = Math.min(0.85d, total / 6.0d);
        if (!trackTags.isEmpty() && !artistTags.isEmpty()) {
            score += 0.1d;
        }
        return toTwoDecimal(clamp01(score));
    }

    private double sumWeights(Map<String, Double> weightedTags, Set<String> targets) {
        return targets.stream()
                .map(weightedTags::get)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private boolean containsAny(Map<String, Double> weightedTags, Set<String> targets) {
        return targets.stream().anyMatch(weightedTags::containsKey);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase()
                .replace("_", " ")
                .replace("-", " ");
        return normalized.replaceAll("\\s+", " ");
    }

    private double clamp01(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        return Math.min(value, 1.0d);
    }

    private BigDecimal toTwoDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record EstimatedMetadataFeatures(
            String mood,
            String genre,
            BigDecimal energy,
            BigDecimal valence,
            String source,
            BigDecimal confidence
    ) {
    }
}
