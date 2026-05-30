package com.playlist.plitter.character.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CharacterDownloadUrlResponse(
        String downloadUrl,
        String imageUrl,
        LocalDateTime expiresAt,
        String characterName,
        List<String> tags
) {
}
