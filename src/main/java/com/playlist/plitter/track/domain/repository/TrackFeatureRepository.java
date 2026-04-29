package com.playlist.plitter.track.domain.repository;

import com.playlist.plitter.track.domain.entity.TrackFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackFeatureRepository extends JpaRepository<TrackFeatureEntity, Long> {
}