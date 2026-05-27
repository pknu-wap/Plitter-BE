package com.playlist.plitter.track.domain.repository;

import com.playlist.plitter.track.domain.entity.TrackFeatureEntity;
import com.playlist.plitter.track.domain.entity.TrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackFeatureRepository extends JpaRepository<TrackFeatureEntity, Long> {
    Optional<TrackFeatureEntity> findByTrack(TrackEntity track);
}
