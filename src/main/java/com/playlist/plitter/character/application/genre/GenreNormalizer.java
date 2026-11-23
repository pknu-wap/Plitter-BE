package com.playlist.plitter.character.application.genre;

import java.util.Locale;

public final class GenreNormalizer {
    private GenreNormalizer() {}

    public static String normalize(String value) {
        String genre = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ");
        return switch (genre) {
            case "k pop", "kpop" -> "K_POP";
            case "hip hop", "hiphop", "rap" -> "HIP_HOP";
            case "r&b", "rnb", "r and b" -> "RNB";
            case "electronic", "edm", "dance", "electronic dance" -> "ELECTRONIC_DANCE";
            case "pop", "rock", "jazz", "ballad", "indie", "classical", "acoustic", "folk", "metal" ->
                    genre.toUpperCase(Locale.ROOT);
            case "", "balanced", "unknown" -> "UNKNOWN";
            default -> "OTHER";
        };
    }
}
