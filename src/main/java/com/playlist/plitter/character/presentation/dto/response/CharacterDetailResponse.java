package com.playlist.plitter.character.presentation.dto.response;

import java.util.List;

public record CharacterDetailResponse(
        Long characterId,
        String imageUrl,
        String characterName,
        List<String> tags
) {
}
