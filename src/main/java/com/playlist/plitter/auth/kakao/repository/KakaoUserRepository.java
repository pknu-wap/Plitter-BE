package com.playlist.plitter.auth.kakao.repository;

import com.playlist.plitter.auth.kakao.entity.KakaoUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoUserRepository extends JpaRepository<KakaoUserEntity, Long> {

    Optional<KakaoUserEntity> findByKakaoId(String kakaoId);

    boolean existsByKakaoId(String kakaoId);

}