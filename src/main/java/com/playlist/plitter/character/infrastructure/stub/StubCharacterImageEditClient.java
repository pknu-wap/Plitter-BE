package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;
import org.springframework.stereotype.Component;

@Component
public class StubCharacterImageEditClient implements CharacterImageEditClient {

    @Override
    public String editCharacterImage(CharacterImageEditRequest request) {
        return "https://cdn.plitter.local/edited/"
                + request.baseCharacterImage().templateId()
                + "-"
                + request.editSpec().styleTone()
                + ".png";
    }
}
