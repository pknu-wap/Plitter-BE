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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lastfm-live-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@EnabledIfEnvironmentVariable(named = "LASTFM_LIVE_TEST", matches = "true")
class TrackFeatureEnrichmentLiveTest {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackFeatureRepository trackFeatureRepository;

    @Autowired
    private TrackFeatureEnrichmentService trackFeatureEnrichmentService;

    @Test
    void enrichTrackFeature_collectsAndSavesMetadataFromLastFm() {
        TrackEntity track = trackRepository.save(TrackEntity.builder()
                .spotifyTrackId("live-test-" + UUID.randomUUID())
                .title("Paranoid Android")
                .artistName("Radiohead")
                .build());

        trackFeatureEnrichmentService.enrichTrackFeature(track.getId());

        Optional<TrackFeatureEntity> saved = trackFeatureRepository.findByTrack(track);
        assertThat(saved).isPresent();

        TrackFeatureEntity feature = saved.get();
        assertThat(feature.getRawFeatureJson()).isNotBlank();
        assertThat(feature.getFetchedAt()).isNotNull();
        assertThat(feature.getRawFeatureJson()).contains("toptags");
        assertThat(feature.getRawFeatureJson()).contains("trackTopTags");
        assertThat(feature.getRawFeatureJson()).contains("artistTopTags");
        assertThat(feature.getFeatureSource()).isEqualTo("LASTFM_TAG");
        assertThat(feature.getConfidence()).isNotNull();
        assertThat(feature.getMood()).isNotBlank();
        assertThat(feature.getGenre()).isNotBlank();
        assertThat(feature.getEnergy()).isNotNull();
        assertThat(feature.getValence()).isNotNull();
    }
}
