package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.BaseCharacterClient;
import com.playlist.plitter.character.application.port.dto.BaseCharacterImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StubBaseCharacterClient implements BaseCharacterClient {

    private final String templateId;
    private final String imageUrl;

    public StubBaseCharacterClient(
            @Value("${character.base-character.template-id:default-base}") String templateId,
            @Value("${character.base-character.image-url:classpath:base_character.png}") String imageUrl
    ) {
        this.templateId = templateId;
        this.imageUrl = imageUrl;
    }

    @Override
    public BaseCharacterImage getBaseCharacter() {
        return new BaseCharacterImage(templateId, imageUrl);
    }
}
