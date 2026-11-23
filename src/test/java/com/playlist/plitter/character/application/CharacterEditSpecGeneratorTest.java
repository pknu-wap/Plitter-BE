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
