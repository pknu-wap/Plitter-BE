package com.playlist.plitter.track.infrastructure.spotify;

import java.math.BigDecimal;

public record SpotifyTrackFeatureResponse(
        BigDecimal bpm,
        String genre,
        BigDecimal energy,
        BigDecimal valence,
        String rawFeatureJson
) {
}
