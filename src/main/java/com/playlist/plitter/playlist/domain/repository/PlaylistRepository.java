package com.playlist.plitter.playlist.domain.repository;

import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Long> {
}
