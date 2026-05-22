package com.playlist.plitter.character.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.port.MusicFeatureClient;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import com.playlist.plitter.recommendations.domain.repository.RecommendationsRepository;
import com.playlist.plitter.track.domain.entity.TrackFeatureEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Primary
@Component
@RequiredArgsConstructor
public class RecommendationMusicFeatureClient implements MusicFeatureClient {

    private final PlaylistRepository playlistRepository;
    private final RecommendationsRepository recommendationsRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String collectFeatures(Long playlistId) {
        PlaylistEntity playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ApiException(CharacterErrorCode.PLAYLIST_NOT_FOUND));

        List<RecommendationsEntity> recommendations = recommendationsRepository.findAllByPlaylist(playlist);
        if (recommendations.isEmpty()) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        List<Long> trackIds = recommendations.stream()
                .map(recommendation -> recommendation.getTrack().getId())
                .distinct()
                .toList();

        List<TrackFeatureEntity> featureEntities = findTrackFeatures(trackIds);
        Map<Long, TrackFeatureEntity> featureByTrackId = featureEntities.stream()
                .collect(Collectors.toMap(
                        feature -> feature.getTrack().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        SummaryAccumulator summary = summarize(recommendations, featureByTrackId);
        return toSummaryJson(playlistId, trackIds.size(), featureByTrackId.size(), summary);
    }

    private List<TrackFeatureEntity> findTrackFeatures(List<Long> trackIds) {
        if (trackIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select tf from TrackFeatureEntity tf where tf.track.id in :trackIds",
                        TrackFeatureEntity.class
                )
                .setParameter("trackIds", trackIds)
                .getResultList();
    }

    private SummaryAccumulator summarize(
            List<RecommendationsEntity> recommendations,
            Map<Long, TrackFeatureEntity> featureByTrackId
    ) {
        SummaryAccumulator summary = new SummaryAccumulator();
        for (RecommendationsEntity recommendation : recommendations) {
            TrackFeatureEntity feature = featureByTrackId.get(recommendation.getTrack().getId());
            if (feature == null) {
                continue;
            }
            if (feature.getBpm() != null) {
                summary.bpmSum += feature.getBpm().doubleValue();
                summary.bpmCount++;
            }
            if (feature.getEnergy() != null) {
                summary.energySum += feature.getEnergy().doubleValue();
                summary.energyCount++;
            }
            if (feature.getValence() != null) {
                summary.valenceSum += feature.getValence().doubleValue();
                summary.valenceCount++;
            }
            String genre = normalizeGenre(feature.getGenre());
            if (genre != null) {
                summary.genreCounts.merge(genre, 1, Integer::sum);
            }
        }
        return summary;
    }

    private String toSummaryJson(
            Long playlistId,
            int trackCount,
            int analyzedTrackCount,
            SummaryAccumulator summary
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playlistId", playlistId);
        payload.put("trackCount", trackCount);
        payload.put("analyzedTrackCount", analyzedTrackCount);
        payload.put("avgBpm", average(summary.bpmSum, summary.bpmCount));
        payload.put("avgEnergy", average(summary.energySum, summary.energyCount));
        payload.put("avgValence", average(summary.valenceSum, summary.valenceCount));
        payload.put("primaryGenre", findPrimaryGenre(summary.genreCounts));
        payload.put("genreDistribution", summary.genreCounts);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private Double average(double sum, int count) {
        if (count == 0) {
            return 0.0;
        }
        return Math.round((sum / count) * 100.0) / 100.0;
    }

    private String findPrimaryGenre(Map<String, Integer> genreCounts) {
        return genreCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("balanced");
    }

    private String normalizeGenre(String genre) {
        if (!StringUtils.hasText(genre)) {
            return null;
        }
        return genre.trim().toLowerCase();
    }

    private static class SummaryAccumulator {
        private double bpmSum;
        private int bpmCount;
        private double energySum;
        private int energyCount;
        private double valenceSum;
        private int valenceCount;
        private final Map<String, Integer> genreCounts = new LinkedHashMap<>();
    }
}
