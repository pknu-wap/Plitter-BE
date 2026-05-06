package com.playlist.plitter.character.application.port.dto;

import java.time.LocalDateTime;

public record DownloadUrlResult(
        String downloadUrl,
        LocalDateTime expiresAt
) {
}
