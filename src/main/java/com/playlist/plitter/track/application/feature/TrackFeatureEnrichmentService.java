package com.playlist.plitter.track.application.feature;

import com.playlist.plitter.track.domain.entity.TrackEntity;
import com.playlist.plitter.track.domain.entity.TrackFeatureEntity;
import com.playlist.plitter.track.domain.repository.TrackFeatureRepository;
import com.playlist.plitter.track.domain.repository.TrackRepository;
import com.playlist.plitter.track.infrastructure.lastfm.LastFmTagBundle;
import com.playlist.plitter.track.infrastructure.lastfm.LastFmTagClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackFeatureEnrichmentService {

    private final TrackRepository trackRepository;
    private final TrackFeatureRepository trackFeatureRepository;
    private final LastFmTagClient lastFmTagClient;
    private final MetadataTagFeatureEstimator metadataTagFeatureEstimator;

    @Transactional
    public void enrichTrackFeature(Long trackId) {
        TrackEntity track = trackRepository.findById(trackId).orElse(null);
        if (track == null) {
            return;
        }

        LastFmTagBundle tagBundle = lastFmTagClient.fetchTopTags(track.getTitle(), track.getArtistName());
        MetadataTagFeatureEstimator.EstimatedMetadataFeatures estimated =
                metadataTagFeatureEstimator.estimate(tagBundle.trackTags(), tagBundle.artistTags());

        TrackFeatureEntity trackFeature = trackFeatureRepository.findByTrack(track)
                .orElseGet(() -> TrackFeatureEntity.builder().track(track).build());

        trackFeature.updateMetadataFeatures(
                null,
                estimated.mood(),
                estimated.genre(),
                estimated.energy(),
                estimated.valence(),
                estimated.source(),
                estimated.confidence(),
                tagBundle.rawJson(),
                LocalDateTime.now()
        );

        trackFeatureRepository.save(trackFeature);
        log.info("Track metadata feature updated. trackId={}, source={}, confidence={}",
                trackId, estimated.source(), estimated.confidence());
    }
}
