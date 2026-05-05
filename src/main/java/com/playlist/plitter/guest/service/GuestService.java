package com.playlist.plitter.guest.service;

import com.playlist.plitter.guest.dto.GuestCreateResponse;
import com.playlist.plitter.guest.entity.GuestUserEntity;
import com.playlist.plitter.guest.repository.GuestUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestUserRepository guestUserRepository;

    public GuestCreateResponse createGuest() {
        String guestToken = generateUniqueToken();
        String nickname = NicknameGenerator.generate(guestUserRepository);

        GuestUserEntity guest = GuestUserEntity.builder()
                .guestToken(guestToken)
                .randomNickname(nickname)
                .build();

        guestUserRepository.save(guest);

        return new GuestCreateResponse(guestToken, nickname);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (guestUserRepository.existsByGuestToken(token));
        return token;
    }
}
