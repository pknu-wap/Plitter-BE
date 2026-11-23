package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import com.playlist.plitter.character.application.dto.MusicProfile;
import com.playlist.plitter.character.application.genre.CharacterGenreTag;
import com.playlist.plitter.character.application.port.CharacterMetadataClient;
import com.playlist.plitter.character.application.port.dto.CharacterMetadataRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CharacterCreativeMetadataGeneratorTest {
    private final CharacterConceptCatalog catalog = new CharacterConceptCatalog();
    private final CharacterResultMetadataGenerator fallback = new CharacterResultMetadataGenerator(catalog);
    private final CharacterConcept concept = new CharacterConceptGenerator(catalog).generate(
            new MusicProfile(MusicProfile.Mood.LOW_LOW, "BALLAD", .25, .3, 5, 5, 5), 42, List.of());

    @Test void generatesNameAndTwoTagsInOneCallAndSuppliesGenreOnServer() {
        List<CharacterMetadataRequest> requests = new ArrayList<>();
        var generator = generator(request -> {
            requests.add(request);
            return creative("전주부터 사연 있는 스타", "새벽감성", "혼자엔딩중");
        });
        var recent = List.of(new CharacterResultMetadata("괜히 창밖 보는 스타", List.of("#발라드", "#느좋", "#무한반복")));

        var result = generator.generate(concept, recent);

        assertEquals("전주부터 사연 있는 스타", result.characterName());
        assertEquals(List.of("#발라드", "#새벽감성", "#혼자엔딩중"), result.tags());
        assertEquals(1, requests.size());
        var request = requests.get(0);
        assertEquals(concept.profile(), request.musicProfile());
        assertEquals("#발라드", request.genreTag());
        assertEquals(catalog.data().props().get(concept.propId()).directive(), request.prop());
        assertEquals(recent, request.recentResults());
    }

    @Test void retriesAnInvalidCandidateOnceWithFeedback() {
        List<CharacterMetadataRequest> requests = new ArrayList<>();
        var generator = generator(request -> {
            requests.add(request);
            return requests.size() == 1 ? creative("스타", "새벽감성", "혼자엔딩중")
                    : creative("전주부터 사연 있는 스타", "새벽감성", "혼자엔딩중");
        });
        var result = generator.generate(concept, List.of());
        assertEquals("전주부터 사연 있는 스타", result.characterName());
        assertEquals(2, requests.size());
        assertFalse(requests.get(1).feedback().isBlank());
    }

    @ParameterizedTest
    @MethodSource("invalidCandidates")
    void invalidOutputsFallBackAfterAtMostTwoCalls(CharacterResultMetadata candidate) {
        var calls = new AtomicInteger();
        var result = generator(request -> { calls.incrementAndGet(); return candidate; }).generate(concept, List.of());
        assertEquals(2, calls.get());
        assertEquals(fallback.generate(concept).characterName(), result.characterName());
        assertEquals("#발라드", result.tags().get(0));
        assertEquals(3, result.tags().stream().distinct().count());
    }

    static Stream<CharacterResultMetadata> invalidCandidates() {
        return Stream.of(
                null,
                new CharacterResultMetadata(null, List.of("새벽감성", "혼자엔딩중")),
                creative("전주부터 사연 있는 별", "새벽감성", "혼자엔딩중"),
                creative("너무너무 길어서 화면에 전부 담기 어려운 스타", "새벽감성", "혼자엔딩중"),
                creative("괜히 센치한 스타", "새벽감성", "새벽감성"),
                creative("괜히 센치한 스타", "POP", "혼자엔딩중"),
                creative("괜히 센치한 스타", "힙합", "혼자엔딩중"),
                creative("괜히 센치한 스타", "느 좋", "혼자엔딩중"),
                creative("괜히 센치한 스타", "무조건열한글자를넘기는아주긴태그", "혼자엔딩중"),
                creative("괜히 센치한 스타", null, "혼자엔딩중"),
                new CharacterResultMetadata("괜히 센치한 스타", null),
                new CharacterResultMetadata("괜히 센치한 스타", List.of("느좋", "혼자엔딩중", "과몰입")));
    }

    @Test void rejectsRecentNamesAndUnorderedTagPairs() {
        var recent = List.of(new CharacterResultMetadata("전주부터 사연 있는 스타",
                List.of("#발라드", "#새벽감성", "#혼자엔딩중")));
        var calls = new AtomicInteger();
        var result = generator(request -> {
            int attempt = calls.incrementAndGet();
            return attempt == 1 ? creative("전주부터 사연 있는 스타", "느좋", "감정충전")
                    : creative("괜히 창밖 보는 스타", "혼자엔딩중", "새벽감성");
        }).generate(concept, recent);
        assertEquals(2, calls.get());
        assertEquals(fallback.generate(concept).characterName(), result.characterName());
    }

    @Test void providerFailureUsesFallbackWithoutRepeatingNetworkCall() {
        var calls = new AtomicInteger();
        var result = generator(request -> { calls.incrementAndGet(); throw new IllegalStateException("provider unavailable"); })
                .generate(concept, List.of());
        assertEquals(1, calls.get());
        assertEquals("#발라드", result.tags().get(0));
    }

    @Test void disabledOrUnconfiguredClientUsesFallbackWithoutCallingProvider() {
        var result = generator(new CharacterMetadataClient() {
            public boolean isAvailable() { return false; }
            public CharacterResultMetadata generate(CharacterMetadataRequest request) { fail("Provider must not be called"); return null; }
        }).generate(concept, List.of());
        assertEquals("#발라드", result.tags().get(0));
        assertEquals(fallback.generate(concept).characterName(), result.characterName());
    }

    @ParameterizedTest
    @CsvSource({"hip-hop,#힙합", "K_POP,#케이팝", "RNB,#알앤비", "electronic,#댄스",
            "rock,#록", "jazz,#재즈", "ballad,#발라드", "indie,#인디", "unknown,#장르믹스", "OTHER,#장르믹스"})
    void normalizesServerGenreLabels(String raw, String expected) {
        assertEquals(expected, CharacterGenreTag.from(raw));
    }

    private CharacterCreativeMetadataGenerator generator(CharacterMetadataClient client) {
        return new CharacterCreativeMetadataGenerator(catalog, fallback, client);
    }

    private static CharacterResultMetadata creative(String name, String moodTag, String characterTag) {
        return new CharacterResultMetadata(name, java.util.Arrays.asList(moodTag, characterTag));
    }
}
