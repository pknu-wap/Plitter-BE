package com.playlist.plitter.character.infrastructure.stub;

import com.playlist.plitter.character.application.port.CharacterAiClient;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpecRequest;
import org.springframework.stereotype.Component;

@Component
public class StubCharacterAiClient implements CharacterAiClient {

    @Override
    public CharacterEditSpec createEditSpec(CharacterEditSpecRequest request) {
        return new CharacterEditSpec(
                "Apply music features to the base character: " + request.featureSummaryJson(),
                "vibrant"
        );
    }
}
