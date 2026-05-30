package com.playlist.plitter.character.presentation.dto.response;

import java.util.List;

public record CharacterCreateResponse(
        Long characterId,
        String imageUrl,
        String characterName,
        List<String> tags
) {
}
