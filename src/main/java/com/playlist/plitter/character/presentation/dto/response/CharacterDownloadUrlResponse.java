package com.playlist.plitter.character.presentation.dto.response;

import java.time.LocalDateTime;

public record CharacterDownloadUrlResponse(
        String downloadUrl,
        String imageUrl,
        LocalDateTime expiresAt
) {
}
