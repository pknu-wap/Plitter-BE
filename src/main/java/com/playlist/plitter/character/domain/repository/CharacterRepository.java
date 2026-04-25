package com.playlist.plitter.character.domain.repository;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
    Optional<CharacterEntity> findTopByPlaylist_IdOrderByVersionDesc(Long playlistId);
}
