package com.playlist.plitter.character.application.port.dto;

import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.dto.MusicProfile;

import java.util.List;

public record CharacterMetadataRequest(
        MusicProfile musicProfile, String genreTag, String expression, String pose,
        String prop, String marks, List<CharacterResultMetadata> recentResults, String feedback
) {
    public CharacterMetadataRequest {
        recentResults = List.copyOf(recentResults);
    }

    public CharacterMetadataRequest withFeedback(String feedback) {
        return new CharacterMetadataRequest(musicProfile, genreTag, expression, pose, prop, marks,
                recentResults, feedback);
    }
}
