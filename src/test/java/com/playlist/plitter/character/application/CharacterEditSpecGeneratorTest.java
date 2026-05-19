package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterEditSpecGeneratorTest {

    private final CharacterEditSpecGenerator generator = new CharacterEditSpecGenerator();

    @Test
    void generate_returnsEnergeticBright_whenBpmAndEnergyAreHigh() {
        String summaryJson = """
                {
                  "avgBpm": 130.2,
                  "avgEnergy": 0.78,
                  "avgValence": 0.72,
                  "primaryGenre": "dance"
                }
                """;

        CharacterEditSpec spec = generator.generate(summaryJson);

        assertEquals("energetic-bright", spec.styleTone());
        assertTrue(spec.promptText().contains("dance"));
    }

    @Test
    void generate_returnsCalmDeep_whenValenceIsLow() {
        String summaryJson = """
                {
                  "avgBpm": 95.0,
                  "avgEnergy": 0.34,
                  "avgValence": 0.20,
                  "primaryGenre": "ballad"
                }
                """;

        CharacterEditSpec spec = generator.generate(summaryJson);

        assertEquals("calm-deep", spec.styleTone());
        assertTrue(spec.promptText().contains("ballad"));
    }
}
