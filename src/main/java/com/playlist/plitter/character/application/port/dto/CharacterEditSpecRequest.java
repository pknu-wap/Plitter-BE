package com.playlist.plitter.character.application.port.dto;

public record CharacterEditSpecRequest(
        Long playlistId,
        String featureSummaryJson
) {
}
