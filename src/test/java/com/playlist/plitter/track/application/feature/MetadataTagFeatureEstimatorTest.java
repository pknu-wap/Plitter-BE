package com.playlist.plitter.track.application.feature;

import com.playlist.plitter.track.infrastructure.lastfm.LastFmTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataTagFeatureEstimatorTest {

    private final MetadataTagFeatureEstimator estimator = new MetadataTagFeatureEstimator();

    @Test
    void estimate_returnsEnergeticProfile_forDanceTags() {
        MetadataTagFeatureEstimator.EstimatedMetadataFeatures result = estimator.estimate(
                List.of(
                        new LastFmTag("dance", 100),
                        new LastFmTag("party", 88),
                        new LastFmTag("happy", 72)
                ),
                List.of(new LastFmTag("pop", 70))
        );

        assertThat(result.genre()).isIn("dance", "pop");
        assertThat(result.mood()).isIn("uplifting", "intense");
        assertThat(result.energy()).isNotNull();
        assertThat(result.energy().doubleValue()).isGreaterThan(0.60d);
        assertThat(result.valence()).isNotNull();
        assertThat(result.valence().doubleValue()).isGreaterThan(0.50d);
        assertThat(result.confidence().doubleValue()).isGreaterThan(0.0d);
    }

    @Test
    void estimate_returnsFallback_whenTagsAreEmpty() {
        MetadataTagFeatureEstimator.EstimatedMetadataFeatures result = estimator.estimate(List.of(), List.of());

        assertThat(result.genre()).isEqualTo("balanced");
        assertThat(result.mood()).isEqualTo("balanced");
        assertThat(result.energy()).isNull();
        assertThat(result.valence()).isNull();
        assertThat(result.source()).isEqualTo("LASTFM_EMPTY");
        assertThat(result.confidence().doubleValue()).isEqualTo(0.0d);
    }
}
