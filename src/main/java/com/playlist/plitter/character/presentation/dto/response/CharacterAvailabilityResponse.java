package com.playlist.plitter.character.presentation.dto.response;

public record CharacterAvailabilityResponse(
        boolean available,
        int requiredCount,
        int currentCount,
        int missingCount
) {
}
