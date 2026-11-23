package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CharacterResultMetadataGenerator {
    private final CharacterConceptCatalog catalog;

    public CharacterResultMetadata generate(CharacterConcept concept) {
        if (!catalog.data().version().equals(concept.catalogVersion()))
            throw new IllegalArgumentException("Unsupported metadata catalog");
        var mood = catalog.mood(concept.profile().mood());
        for (int i = 0; i < 3; i++) {
            if (concept.tagIds().size() != 3 || !mood.tags().get(i).contains(concept.tagIds().get(i)))
                throw new IllegalArgumentException("Tags do not match concept");
        }
        return new CharacterResultMetadata(catalog.name(concept.profile().mood(), concept.nameId()),
                concept.tagIds().stream().map(tag -> "#" + tag).toList());
    }
}
