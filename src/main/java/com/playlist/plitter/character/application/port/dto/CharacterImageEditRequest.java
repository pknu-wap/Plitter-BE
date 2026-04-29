package com.playlist.plitter.character.application.port.dto;

public record CharacterImageEditRequest(
        BaseCharacterImage baseCharacterImage,
        CharacterEditSpec editSpec
) {
}
