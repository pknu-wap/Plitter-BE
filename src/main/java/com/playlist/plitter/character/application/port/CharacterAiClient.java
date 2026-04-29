package com.playlist.plitter.character.application.port;

import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpecRequest;

public interface CharacterAiClient {
    CharacterEditSpec createEditSpec(CharacterEditSpecRequest request);
}
