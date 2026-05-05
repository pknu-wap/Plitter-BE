package com.playlist.plitter.guest.repository;

import com.playlist.plitter.guest.entity.GuestUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestUserRepository extends JpaRepository<GuestUserEntity, Long> {
    boolean existsByGuestToken(String guestToken);
    boolean existsByRandomNickname(String randomNickname);
    boolean existsByRandomNicknameStartingWith(String prefix);
}