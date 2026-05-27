package com.playlist.plitter.track.application.feature;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackFeatureBackfillService {

    private final EntityManager entityManager;
    private final TrackFeatureEnrichmentService trackFeatureEnrichmentService;

    @Transactional(readOnly = true)
    public List<Long> findBackfillCandidateTrackIds(int limit) {
        return entityManager.createQuery("""
                        select t.id
                        from TrackEntity t
                        left join TrackFeatureEntity tf on tf.track.id = t.id
                        where tf is null
                           or tf.featureSource is null
                           or (tf.genre = 'unknown' and tf.mood is null and tf.energy is null and tf.valence is null)
                        order by t.id desc
                        """, Long.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public BackfillResult backfill(int limit) {
        List<Long> trackIds = findBackfillCandidateTrackIds(limit);
        int successCount = 0;
        for (Long trackId : trackIds) {
            try {
                trackFeatureEnrichmentService.enrichTrackFeature(trackId);
                successCount++;
            } catch (Exception exception) {
                log.warn("Track feature backfill failed. trackId={}", trackId, exception);
            }
        }

        return new BackfillResult(trackIds.size(), successCount);
    }

    public record BackfillResult(int candidateCount, int successCount) {
    }
}
