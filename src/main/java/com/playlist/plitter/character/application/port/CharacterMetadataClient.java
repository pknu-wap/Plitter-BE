package com.playlist.plitter.character.application.port;

import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.port.dto.CharacterMetadataRequest;

public interface CharacterMetadataClient {
    // Returns a name and two tags: mood, then personality/situation. The server supplies the genre.
    CharacterResultMetadata generate(CharacterMetadataRequest request);

    default boolean isAvailable() { return true; }
}
