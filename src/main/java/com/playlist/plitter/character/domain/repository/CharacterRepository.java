package com.playlist.plitter.character.domain.repository;

import com.playlist.plitter.character.domain.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
}
