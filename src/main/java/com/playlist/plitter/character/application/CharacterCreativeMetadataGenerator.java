package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.genre.CharacterGenreTag;
import com.playlist.plitter.character.application.port.CharacterMetadataClient;
import com.playlist.plitter.character.application.port.dto.CharacterMetadataRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CharacterCreativeMetadataGenerator {
    private static final Logger log = LoggerFactory.getLogger(CharacterCreativeMetadataGenerator.class);

    private final CharacterConceptCatalog catalog;
    private final CharacterResultMetadataGenerator fallbackGenerator;
    private final CharacterMetadataClient client;

    public CharacterResultMetadata generate(CharacterConcept concept, List<CharacterResultMetadata> recent) {
        String genreTag = CharacterGenreTag.from(concept.profile().genre());
        var fallback = fallbackGenerator.generate(concept);
        var fallbackResult = new CharacterResultMetadata(fallback.characterName(),
                List.of(genreTag, fallback.tags().get(0), fallback.tags().get(2)));
        if (!client.isAvailable()) {
            log.info("Character metadata fallback: reason=client_unavailable");
            return fallbackResult;
        }

        var data = catalog.data();
        var request = new CharacterMetadataRequest(concept.profile(), genreTag,
                data.expressions().get(concept.expressionId()).directive(),
                data.poses().get(concept.poseId()).directive(),
                data.props().get(concept.propId()).directive(),
                data.marks().get(concept.marksId()).directive(), recent.stream().limit(3).toList(), "");

        for (int attempt = 0; attempt < 2; attempt++) {
            CharacterResultMetadata generated;
            try {
                generated = client.generate(request);
            } catch (Exception e) {
                // Do not log provider response bodies: they can contain generated text or credentials.
                log.warn("Character metadata fallback: reason=provider_error, failure={}",
                        e.getClass().getSimpleName());
                return fallbackResult;
            }
            try {
                CharacterResultMetadata result = validateAndComplete(generated, genreTag, request.recentResults());
                log.info("Character metadata accepted: source=llm");
                return result;
            } catch (IllegalArgumentException e) {
                log.warn("Character metadata validation failed: attempt={}, rule={}", attempt + 1, e.getMessage());
                request = request.withFeedback(e.getMessage() + " Generate a new name and two new tags.");
            }
        }
        log.warn("Character metadata fallback: reason=validation_exhausted");
        return fallbackResult;
    }

    private CharacterResultMetadata validateAndComplete(CharacterResultMetadata result, String genreTag,
                                                         List<CharacterResultMetadata> recent) {
        if (result == null) throw new IllegalArgumentException("A name and two tags are required.");
        String name = normalize(result.characterName());
        if (name.length() > 18 || !name.matches("[가-힣A-Za-z0-9 ]+ 스타"))
            throw new IllegalArgumentException("Use a name ending in ' 스타', up to 18 characters.");
        if (result.tags() == null || result.tags().size() != 2)
            throw new IllegalArgumentException("Return exactly two tags: mood and personality/situation.");
        List<String> tags = result.tags().stream().map(CharacterCreativeMetadataGenerator::normalizeTag).toList();
        for (String tag : tags) {
            if (tag.length() > 11 || !tag.matches("[가-힣A-Za-z0-9]+"))
                throw new IllegalArgumentException("Each tag must contain 1-11 letters or digits, with no spaces or punctuation.");
            if (CharacterGenreTag.isGenre(tag))
                throw new IllegalArgumentException("The server supplies the genre; generate mood and personality/situation tags only.");
        }
        if (new HashSet<>(tags.stream().map(t -> t.toLowerCase(Locale.ROOT)).toList()).size() != 2)
            throw new IllegalArgumentException("The two tags must be different.");
        for (CharacterResultMetadata previous : recent) {
            if (name.equals(normalize(previous.characterName())))
                throw new IllegalArgumentException("Do not reuse a recent character name.");
            if (previous.tags() != null && previous.tags().size() == 3) {
                var previousPair = previous.tags().subList(1, 3).stream()
                        .map(CharacterCreativeMetadataGenerator::normalizeTag).map(t -> t.toLowerCase(Locale.ROOT)).toList();
                if (new HashSet<>(previousPair).equals(new HashSet<>(tags.stream()
                        .map(t -> t.toLowerCase(Locale.ROOT)).toList())))
                    throw new IllegalArgumentException("Do not reuse a recent pair of tags.");
            }
        }
        return new CharacterResultMetadata(name, List.of(genreTag, "#" + tags.get(0), "#" + tags.get(1)));
    }

    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC).strip();
    }

    private static String normalizeTag(String value) {
        String tag = normalize(value);
        return tag.startsWith("#") ? tag.substring(1) : tag;
    }
}
