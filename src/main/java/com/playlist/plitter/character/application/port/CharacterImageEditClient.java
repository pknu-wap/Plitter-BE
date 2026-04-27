package com.playlist.plitter.character.application.port;

import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;

public interface CharacterImageEditClient {
    String editCharacterImage(CharacterImageEditRequest request);
}
