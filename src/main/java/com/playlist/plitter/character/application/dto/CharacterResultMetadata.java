package com.playlist.plitter.character.application.dto;

import java.util.List;

public record CharacterResultMetadata(
        String characterName,
        List<String> tags
) {
}
