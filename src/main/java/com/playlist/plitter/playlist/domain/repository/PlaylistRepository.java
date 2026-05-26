package com.playlist.plitter.playlist.domain.repository;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Long> {
    boolean existsByOwner(UserEntity owner);
    Optional<PlaylistEntity> findByOwnerId(Long ownerId);

    @Modifying
    @Query("update PlaylistEntity p set p.recommendationCount = p.recommendationCount + 1 where p = :playlist")
    int increaseRecommendationCount(@Param("playlist") PlaylistEntity playlist);
}
