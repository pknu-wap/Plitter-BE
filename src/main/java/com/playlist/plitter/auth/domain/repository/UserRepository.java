package com.playlist.plitter.auth.domain.repository;

import com.playlist.plitter.auth.domain.entity.GuestUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

    public interface UserRepository extends JpaRepository<GuestUserEntity, Long> {
        boolean existsByGuestToken(String guestToken);
    }

