package com.playlist.plitter.auth.domain.repository;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

    public interface UserRepository extends JpaRepository<UserEntity, Long> {
        boolean existsByGuestToken(String guestToken);
    }

