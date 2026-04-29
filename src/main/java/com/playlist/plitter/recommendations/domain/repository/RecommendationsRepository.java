package com.playlist.plitter.recommendations.domain.repository;

import com.playlist.plitter.recommendations.domain.entity.RecommendationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationsRepository extends JpaRepository<RecommendationsEntity, Long> {
}