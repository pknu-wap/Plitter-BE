package com.playlist.plitter.track.domain.repository;

import com.playlist.plitter.track.domain.entity.TrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<TrackEntity, Long> {
    Optional<TrackEntity> findBySpotifyTrackId(String spotifyTrackId);
}
