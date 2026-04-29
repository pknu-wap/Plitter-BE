package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.MusicFeatureClient;
import org.springframework.stereotype.Component;

@Component
public class StubMusicFeatureClient implements MusicFeatureClient {

    @Override
    public String collectFeatures(Long playlistId) {
        return String.format(
                "{\"playlistId\":%d,\"mood\":\"balanced\"}",
                playlistId
        );
    }
}
