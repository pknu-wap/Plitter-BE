package com.playlist.plitter.character.application;

import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CharacterService {

    public CharacterAvailabilityResponse getAvailability(Long playlistId) {
        return new CharacterAvailabilityResponse(false, 10, 0, 10);
    }

    public CharacterCreateResponse createCharacter(Long playlistId) {
        return new CharacterCreateResponse(1L, "https://cdn.plitter.local/characters/" + playlistId + "/v1.png");
    }

    public CharacterDetailResponse getCharacter(Long playlistId) {
        return new CharacterDetailResponse(1L, "https://cdn.plitter.local/characters/" + playlistId + "/v1.png");
    }

    public CharacterDownloadUrlResponse getCharacterDownloadUrl(Long playlistId) {
        String imageUrl = "https://cdn.plitter.local/characters/" + playlistId + "/v1.png";
        return new CharacterDownloadUrlResponse(
                imageUrl + "?download=true",
                imageUrl,
                LocalDateTime.now().plusMinutes(30)
        );
    }
}
