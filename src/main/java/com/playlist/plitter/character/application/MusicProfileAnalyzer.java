package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.MusicProfile;
import com.playlist.plitter.character.application.dto.MusicProfile.Mood;
import com.playlist.plitter.character.application.genre.GenreNormalizer;
import org.springframework.stereotype.Component;

@Component
public class MusicProfileAnalyzer {
    private final ObjectMapper mapper = new ObjectMapper();

    public MusicProfile analyze(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject()) throw new IllegalArgumentException("Expected feature object");
            Double energy = validValue(root.get("avgEnergy"));
            Double valence = validValue(root.get("avgValence"));
            int energyCount = Math.max(0, root.path("energyCount").asInt());
            int valenceCount = Math.max(0, root.path("valenceCount").asInt());
            if (energyCount == 0) energy = null;
            if (valenceCount == 0) valence = null;
            Mood mood = energy == null || valence == null || energyCount < 2 || valenceCount < 2
                    ? Mood.UNKNOWN : Mood.valueOf(band(energy, 0.65) + "_" + band(valence, 0.60));
            return new MusicProfile(mood, GenreNormalizer.normalize(root.path("primaryGenre").asText()),
                    energy, valence, energyCount, valenceCount, Math.max(0, root.path("trackCount").asInt()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid music feature summary", e);
        }
    }

    private static String band(double value, double high) {
        return value < 0.40 ? "LOW" : value < high ? "MID" : "HIGH";
    }

    private static Double validValue(JsonNode node) {
        if (node == null || !node.isNumber()) return null;
        double value = node.asDouble();
        return Double.isFinite(value) && value >= 0 && value <= 1 ? value : null;
    }
}
