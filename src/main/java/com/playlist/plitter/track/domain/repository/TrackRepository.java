package com.playlist.plitter.track.domain.repository;

import com.playlist.plitter.track.domain.entity.TrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRepository extends JpaRepository<TrackEntity, Long> {
}
