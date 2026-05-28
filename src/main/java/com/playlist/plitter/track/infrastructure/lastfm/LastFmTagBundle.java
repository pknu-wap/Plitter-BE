package com.playlist.plitter.track.infrastructure.lastfm;

import java.util.List;

public record LastFmTagBundle(
        List<LastFmTag> trackTags,
        List<LastFmTag> artistTags,
        String rawJson
) {
}
