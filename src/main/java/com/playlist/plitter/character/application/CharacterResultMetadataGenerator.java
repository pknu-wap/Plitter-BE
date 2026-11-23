package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.genre.CharacterGenreTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
        // Keep all three V2 tag IDs valid for stored concept history. Display genre, mood and reaction.
        return new CharacterResultMetadata(catalog.name(concept.profile().mood(), concept.nameId()), List.of(
                CharacterGenreTag.from(concept.profile().genre()),
                "#" + concept.tagIds().get(0), "#" + concept.tagIds().get(2)));
    }
}
