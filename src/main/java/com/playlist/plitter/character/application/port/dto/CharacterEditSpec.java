package com.playlist.plitter.character.application.port.dto;

public record CharacterEditSpec(
        String promptText,
        String styleTone
) {
}
