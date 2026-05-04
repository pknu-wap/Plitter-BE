package com.playlist.plitter.recommendations.domain.repository;

import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationsRepository extends JpaRepository<RecommendationsEntity, Long> {
    List<RecommendationsEntity> findAllByPlaylist(PlaylistEntity playlist);

    boolean existsByPlaylistAndTrack_SpotifyTrackIdAndComment(
            PlaylistEntity playlist,
            String spotifyTrackId,
            String comment
    );
}
