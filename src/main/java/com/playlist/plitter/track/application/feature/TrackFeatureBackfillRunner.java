package com.playlist.plitter.track.application.feature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackFeatureBackfillRunner implements ApplicationRunner {

    private final TrackFeatureBackfillService trackFeatureBackfillService;

    @Value("${track.feature.backfill.enabled:false}")
    private boolean enabled;

    @Value("${track.feature.backfill.limit:50}")
    private int limit;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        TrackFeatureBackfillService.BackfillResult result = trackFeatureBackfillService.backfill(limit);
        log.info("Track feature backfill finished. candidates={}, success={}",
                result.candidateCount(), result.successCount());
    }
}
