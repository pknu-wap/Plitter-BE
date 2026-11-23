package com.playlist.plitter.character.application.genre;

import java.util.Map;

public final class CharacterGenreTag {
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("K_POP", "케이팝"), Map.entry("HIP_HOP", "힙합"),
            Map.entry("RNB", "알앤비"), Map.entry("ELECTRONIC_DANCE", "댄스"),
            Map.entry("POP", "팝"), Map.entry("ROCK", "록"), Map.entry("JAZZ", "재즈"),
            Map.entry("BALLAD", "발라드"), Map.entry("INDIE", "인디"),
            Map.entry("CLASSICAL", "클래식"), Map.entry("ACOUSTIC", "어쿠스틱"),
            Map.entry("FOLK", "포크"), Map.entry("METAL", "메탈"));

    private CharacterGenreTag() {}

    public static String from(String genre) {
        return "#" + LABELS.getOrDefault(GenreNormalizer.normalize(genre), "장르믹스");
    }

    public static boolean isGenre(String value) {
        return LABELS.containsValue(value) || value.equals("장르믹스") || value.equals("락")
                || LABELS.containsKey(GenreNormalizer.normalize(value));
    }
}
