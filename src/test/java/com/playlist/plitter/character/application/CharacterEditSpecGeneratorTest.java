package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.*;
import com.playlist.plitter.character.application.dto.MusicProfile.Mood;
import com.playlist.plitter.character.application.genre.GenreNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CharacterEditSpecGeneratorTest {
    private final CharacterConceptCatalog catalog = new CharacterConceptCatalog();
    private final MusicProfileAnalyzer analyzer = new MusicProfileAnalyzer();
    private final CharacterConceptGenerator concepts = new CharacterConceptGenerator(catalog);
    private final CharacterEditSpecGenerator prompts = new CharacterEditSpecGenerator(catalog);
    private final CharacterResultMetadataGenerator metadata = new CharacterResultMetadataGenerator(catalog);

    @ParameterizedTest
    @CsvSource({"0,0,LOW_LOW", "0.399999,0.4,LOW_MID", "0.2,0.6,LOW_HIGH",
            "0.4,0.2,MID_LOW", "0.48,0.4,MID_MID", "0.649999,1,MID_HIGH",
            "0.65,0.399999,HIGH_LOW", "1,0.599999,HIGH_MID", "1,1,HIGH_HIGH"})
    void classifiesBoundaries(double energy, double valence, Mood expected) {
        assertEquals(expected, analyzer.analyze(summary(energy, valence, 5, "dance")).mood());
    }

    @ParameterizedTest
    @CsvSource({"hip hop,HIP_HOP", "hip-hop,HIP_HOP", "hiphop,HIP_HOP", "k pop,K_POP", "K_POP,K_POP",
            "electronic,ELECTRONIC_DANCE", "ELECTRONIC_DANCE,ELECTRONIC_DANCE", "balanced,UNKNOWN", "metal,METAL"})
    void normalizesStoredGenres(String raw, String expected) {
        assertEquals(expected, GenreNormalizer.normalize(raw));
    }

    @ParameterizedTest
    @CsvSource({"hip-hop,#힙합", "K_POP,#케이팝", "RNB,#알앤비", "electronic,#댄스",
            "rock,#록", "jazz,#재즈", "ballad,#발라드", "indie,#인디", "unknown,#장르믹스", "OTHER,#장르믹스"})
    void firstTagShowsTheServerSelectedGenre(String genre, String expected) {
        var c = concepts.generate(analyzer.analyze(summary(.5, .5, 5, genre)), 42, List.of());
        assertEquals(expected, metadata.generate(c).tags().get(0));
    }

    @Test void twentyNamesPerMoodPreserveExistingNameIds() {
        Map<Mood, String> oldLastNames = Map.of(
                Mood.LOW_LOW, "살짝 말랑한 스타", Mood.LOW_MID, "오늘따라 차분한 스타",
                Mood.LOW_HIGH, "살짝 들뜬 스타", Mood.MID_LOW, "대놓고 분위기 있는 스타",
                Mood.MID_MID, "괜히 당당한 스타", Mood.MID_HIGH, "살짝 뻔뻔한 스타",
                Mood.HIGH_LOW, "유난히 뜨거운 스타", Mood.HIGH_MID, "묘하게 멋있는 스타",
                Mood.HIGH_HIGH, "오늘따라 뻔뻔한 스타", Mood.UNKNOWN, "대놓고 자유로운 스타");
        for (Mood mood : Mood.values()) {
            assertEquals(20, catalog.mood(mood).names().size());
            assertEquals(oldLastNames.get(mood), catalog.name(mood, mood.name() + ":5"));
        }
    }

    @Test void recentStoredNamesAndTagPairsAreAvoidedEvenWhenConceptNamesDiffer() {
        var profile = analyzer.analyze(summary(.5, .5, 5, "hip hop"));
        var previous = concepts.generate(profile, 42, List.of());
        var recent = List.of(
                new CharacterResultMetadata("취향 하나는 확실한 스타", List.of("#힙합", "#느좋", "#감다살")),
                new CharacterResultMetadata("무심한데 폼 좋은 스타", List.of("#힙합", "#기분전환", "#추구미")),
                new CharacterResultMetadata("은근 감 좋은 스타", List.of("#힙합", "#칠링", "#취향저격")));
        for (int seed = 0; seed < 200; seed++) {
            var c = concepts.generate(profile, seed, List.of(previous), recent);
            assertEquals(c, concepts.generate(profile, seed, List.of(previous), recent));
            var result = metadata.generate(c);
            for (var old : recent) {
                assertNotEquals(old.characterName(), result.characterName());
                assertNotEquals(new HashSet<>(old.tags().subList(1, 3)), new HashSet<>(result.tags().subList(1, 3)));
            }
        }
    }

    @Test void preGenreTagResultsAlsoParticipateInDuplicateAvoidance() {
        var profile = analyzer.analyze(summary(.2, .2, 5, "ballad"));
        var previous = new CharacterResultMetadata("괜히 센치한 스타", List.of("#새벽감성", "#창가플리", "#무한반복"));
        for (int seed = 0; seed < 200; seed++) {
            var c = concepts.generate(profile, seed, List.of(), List.of(previous));
            var result = metadata.generate(c);
            assertNotEquals(previous.characterName(), result.characterName());
            assertNotEquals(Set.of("#새벽감성", "#무한반복"), new HashSet<>(result.tags().subList(1, 3)));
        }
    }

    @Test void familiarSituationTagsRemainSelectableAlongsideSnsExpressions() {
        Map<Mood, String> favourites = Map.of(Mood.LOW_LOW, "#이불속콘서트",
                Mood.HIGH_LOW, "#방구석헤드라이너", Mood.HIGH_HIGH, "#혼자페스티벌");
        for (var entry : favourites.entrySet()) {
            var profile = new MusicProfile(entry.getKey(), "POP", .5, .5, 5, 5, 5);
            Set<String> selected = new HashSet<>();
            for (int seed = 0; seed < 200; seed++)
                selected.add(metadata.generate(concepts.generate(profile, seed, List.of())).tags().get(2));
            assertTrue(selected.contains(entry.getValue()), entry.getKey() + " lost " + entry.getValue());
        }
    }

    @Test void distinguishesMissingFromRealZero() {
        assertEquals(Mood.LOW_LOW, analyzer.analyze(summary(0, 0, 2, "jazz")).mood());
        assertEquals(Mood.UNKNOWN, analyzer.analyze(summary(0, 0, 0, "jazz")).mood());
        assertNull(analyzer.analyze(summary(0, 0, 0, "jazz")).energy());
        assertEquals(Mood.UNKNOWN, analyzer.analyze(summary(0.8, 0.8, 1, "pop")).mood());
        assertEquals(Mood.UNKNOWN, analyzer.analyze(summary(1.1, 0.8, 5, "pop")).mood());
        assertEquals(Mood.UNKNOWN, analyzer.analyze("{\"avgEnergy\":null,\"avgValence\":null}").mood());
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("[]"));
    }

    @Test void allProfilesHaveDiverseValidConsistentResults() {
        for (Mood mood : Mood.values()) for (String genre : catalog.data().genres().keySet()) {
            var profile = new MusicProfile(mood, genre, .5, .5, 5, 5, 5);
            Set<String> signatures = new HashSet<>();
            for (int seed = 0; seed < 200; seed++) {
                var c = concepts.generate(profile, seed, List.of());
                signatures.add(c.signature());
                var m = metadata.generate(c);
                assertTrue(m.characterName().endsWith(" 스타"));
                assertTrue(m.characterName().length() <= 18);
                assertEquals(3, new HashSet<>(m.tags()).size());
                assertTrue(m.tags().stream().allMatch(t -> t.startsWith("#") && t.length() <= 12));
                assertEquals(mood.name(), prompts.generate(c).styleTone());
                assertTrue(prompts.generate(c).promptText().contains(catalog.data().props().get(c.propId()).directive()));
            }
            assertTrue(signatures.size() >= 16, mood + "/" + genre + " only " + signatures.size());
        }
    }

    @Test void replaysSeedAndAvoidsThreeRecentNamesAndVisuals() {
        var profile = analyzer.analyze(summary(.48, .2, 5, "ballad"));
        List<CharacterConcept> recent = new ArrayList<>();
        for (int n = 0; n < 20; n++) {
            var c = concepts.generate(profile, n, recent);
            assertEquals(c, concepts.generate(profile, n, recent));
            assertTrue(recent.stream().noneMatch(old -> old.signature().equals(c.signature())));
            assertTrue(recent.stream().noneMatch(old -> old.nameId().equals(c.nameId())));
            assertTrue(recent.stream().noneMatch(old -> new HashSet<>(metadata.generate(old).tags().subList(1, 3))
                    .equals(new HashSet<>(metadata.generate(c).tags().subList(1, 3)))));
            assertEquals("MID_LOW", prompts.generate(c).styleTone());
            assertTrue(catalog.mood(Mood.MID_LOW).names().contains(metadata.generate(c).characterName()));
            recent.add(0, c);
            recent = new ArrayList<>(recent.stream().limit(3).toList());
        }
    }

    @Test void unknownDoesNotPretendToKnowEmotion() {
        var c = concepts.generate(analyzer.analyze(summary(0, 0, 0, "jazz")), 9, List.of());
        assertEquals("JAZZ", c.profile().genre());
        assertTrue(metadata.generate(c).tags().stream().noneMatch(t -> t.contains("감성") || t.contains("텐션")));
    }

    @Test void recentDisplayNamesAreAvoidedAcrossDifferentMoods() {
        var previousProfile=analyzer.analyze(summary(.8,.5,5,"hip hop"));
        CharacterConcept previous=null;
        for(int seed=0;seed<1000;seed++) {
            var candidate=concepts.generate(previousProfile,seed,List.of());
            if(metadata.generate(candidate).characterName().equals("대놓고 힙한 스타")) { previous=candidate;break; }
        }
        assertNotNull(previous);
        var currentProfile=analyzer.analyze(summary(.5,.5,5,"hip hop"));
        for(int seed=0;seed<200;seed++) {
            var next=concepts.generate(currentProfile,seed,List.of(previous));
            assertNotEquals("대놓고 힙한 스타",metadata.generate(next).characterName());
        }
    }

    @Test void legacyMetadataIsUnchanged() {
        var old = new LegacyCharacterResultMetadataGenerator().generate(summary(.78, .72, 5, "dance"));
        assertEquals("댄스 스텝 스타", old.characterName());
        assertEquals(List.of("#밝은", "#열정적인", "#쾌활한"), old.tags());
    }

    private static String summary(double energy, double valence, int count, String genre) {
        return "{\"avgEnergy\":" + energy + ",\"avgValence\":" + valence
                + ",\"energyCount\":" + count + ",\"valenceCount\":" + count
                + ",\"trackCount\":5,\"primaryGenre\":\"" + genre + "\"}";
    }
}
