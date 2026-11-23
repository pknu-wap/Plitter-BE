package com.playlist.plitter.character.application.dto;

public record MusicProfile(
        Mood mood, String genre, Double energy, Double valence,
        int energyCount, int valenceCount, int trackCount
) {
    public enum Mood {
        LOW_LOW, LOW_MID, LOW_HIGH, MID_LOW, MID_MID, MID_HIGH,
        HIGH_LOW, HIGH_MID, HIGH_HIGH, UNKNOWN
    }
}
