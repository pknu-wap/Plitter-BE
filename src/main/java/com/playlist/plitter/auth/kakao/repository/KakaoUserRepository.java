package com.playlist.plitter.auth.kakao.repository;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByKakaoId(String kakaoId);

    boolean existsByKakaoId(String kakaoId);

}
