package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.dto.DownloadUrlResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StubImageStorageClient implements ImageStorageClient {

    @Override
    public String storeCharacterImage(Long playlistId, String sourceImageUrl) {
        return "https://cdn.plitter.local/characters/"
                + playlistId
                + "/generated.png";
    }

    @Override
    public DownloadUrlResult createDownloadUrl(String imageUrl) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        String downloadUrl = imageUrl + "?download=true&expiresAt=" + expiresAt;
        return new DownloadUrlResult(downloadUrl, expiresAt);
    }
}
