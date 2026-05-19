package com.playlist.plitter.character.application.port;

import com.playlist.plitter.character.application.port.dto.DownloadUrlResult;

public interface ImageStorageClient {
    String storeCharacterImage(Long playlistId, String sourceImageUrl);
    DownloadUrlResult createDownloadUrl(String imageUrl);
}
