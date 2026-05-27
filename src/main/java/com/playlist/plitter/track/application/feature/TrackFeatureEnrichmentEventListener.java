package com.playlist.plitter.track.application.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TrackFeatureEnrichmentEventListener {

    private final TrackFeatureEnrichmentService trackFeatureEnrichmentService;

    @Async("trackFeatureExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrackFeatureEnrichmentRequested(TrackFeatureEnrichmentRequestedEvent event) {
        trackFeatureEnrichmentService.enrichTrackFeature(event.trackId());
    }
}
