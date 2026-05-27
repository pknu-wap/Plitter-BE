package com.playlist.plitter.track.application.feature;

import com.playlist.plitter.track.domain.entity.TrackEntity;
import com.playlist.plitter.track.domain.entity.TrackFeatureEntity;
import com.playlist.plitter.track.domain.repository.TrackFeatureRepository;
import com.playlist.plitter.track.domain.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LASTFM_PROD_DB_TEST", matches = "true")
class TrackFeatureEnrichmentProdDbSmokeTest {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackFeatureRepository trackFeatureRepository;

    @Autowired
    private TrackFeatureEnrichmentService trackFeatureEnrichmentService;

    @Test
    void enrichTrackFeature_writesIntoRealPostgresAndCleansUp() {
        TrackEntity track = trackRepository.save(TrackEntity.builder()
                .spotifyTrackId("prod-smoke-" + UUID.randomUUID())
                .title("Paranoid Android")
                .artistName("Radiohead")
                .build());

        try {
            trackFeatureEnrichmentService.enrichTrackFeature(track.getId());

            Optional<TrackFeatureEntity> saved = trackFeatureRepository.findByTrack(track);
            assertThat(saved).isPresent();

            TrackFeatureEntity feature = saved.get();
            assertThat(feature.getRawFeatureJson()).isNotBlank();
            assertThat(feature.getFetchedAt()).isNotNull();
            assertThat(feature.getRawFeatureJson()).contains("trackTopTags");
            assertThat(feature.getFeatureSource()).isEqualTo("LASTFM_TAG");
            assertThat(feature.getMood()).isNotBlank();
            assertThat(feature.getGenre()).isNotBlank();
            assertThat(feature.getEnergy()).isNotNull();
            assertThat(feature.getValence()).isNotNull();
        } finally {
            trackFeatureRepository.findByTrack(track).ifPresent(trackFeatureRepository::delete);
            trackRepository.deleteById(track.getId());
        }
    }
}
