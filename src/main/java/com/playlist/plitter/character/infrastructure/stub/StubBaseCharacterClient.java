package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.BaseCharacterClient;
import com.playlist.plitter.character.application.port.dto.BaseCharacterImage;
import org.springframework.stereotype.Component;

@Component
public class StubBaseCharacterClient implements BaseCharacterClient {

    @Override
    public BaseCharacterImage getBaseCharacter() {
        String templateId = "default-base";
        return new BaseCharacterImage(
                templateId,
                "https://cdn.plitter.local/base-characters/" + templateId + ".png"
        );
    }
}
