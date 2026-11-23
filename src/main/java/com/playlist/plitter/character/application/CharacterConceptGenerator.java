package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.dto.MusicProfile;
import com.playlist.plitter.character.application.dto.CharacterResultMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CharacterConceptGenerator {
    private final CharacterConceptCatalog catalog;

    private record Visual(String expression, String pose, String prop, String marks, long weight) {
        String signature() { return String.join("/", expression, pose, prop, marks); }
    }

    public CharacterConcept generate(MusicProfile profile, long seed, List<CharacterConcept> recent) {
        return generate(profile, seed, recent, List.of());
    }

    public CharacterConcept generate(MusicProfile profile, long seed, List<CharacterConcept> recent,
                                     List<CharacterResultMetadata> recentMetadata) {
        var m = catalog.mood(profile.mood());
        var d = catalog.data();
        List<Visual> all = new ArrayList<>();
        for (String e : sorted(m.expressions())) for (String p : sorted(m.poses()))
            for (String prop : sorted(catalog.props(profile.genre()))) for (String mark : sorted(m.marks())) {
                long weight = (long)d.expressions().get(e).weight() * d.poses().get(p).weight()
                        * d.props().get(prop).weight() * d.marks().get(mark).weight();
                all.add(new Visual(e, p, prop, mark, weight));
            }
        List<CharacterConcept> history = recent.stream().limit(3).toList();
        List<Visual> eligible = List.of();
        for (int keep = history.size(); keep >= 0 && eligible.isEmpty(); keep--) {
            Set<String> signatures = new HashSet<>();
            history.subList(0, keep).forEach(c -> signatures.add(c.signature()));
            eligible = all.stream().filter(v -> !signatures.contains(v.signature())).toList();
        }
        Random random = new Random(mixSeed(seed));
        long total = eligible.stream().mapToLong(Visual::weight).sum();
        long ticket = (long)(random.nextDouble() * total);
        Visual selected = eligible.get(eligible.size() - 1);
        for (Visual visual : eligible) {
            ticket -= visual.weight();
            if (ticket < 0) { selected = visual; break; }
        }
        Set<String> recentNames = new HashSet<>();
        Set<String> recentTagPairs = new HashSet<>();
        if (recentMetadata.isEmpty()) {
            history.stream().filter(c -> c.catalogVersion().equals(d.version())).forEach(c -> {
                recentNames.add(catalog.name(c.profile().mood(), c.nameId()));
                recentTagPairs.add(tagPairKey(c.tagIds().get(0), c.tagIds().get(2)));
            });
        } else {
            recentMetadata.stream().limit(3).forEach(metadata -> {
                recentNames.add(metadata.characterName());
                // Pre-genre V2 results contain three descriptive tags. Check every pair for both formats.
                for (int i = 0; i < metadata.tags().size(); i++)
                    for (int j = i + 1; j < metadata.tags().size(); j++)
                        recentTagPairs.add(tagPairKey(metadata.tags().get(i), metadata.tags().get(j)));
            });
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < m.names().size(); i++) {
            String id = profile.mood().name() + ":" + i;
            if (!recentNames.contains(m.names().get(i))) names.add(id);
        }
        if (names.isEmpty()) for (int i = 0; i < m.names().size(); i++) names.add(profile.mood().name() + ":" + i);
        String nameId = names.get(random.nextInt(names.size()));
        List<List<String>> pairs = new ArrayList<>();
        for (String moodTag : sorted(m.tags().get(0))) for (String reactionTag : sorted(m.tags().get(2)))
            pairs.add(List.of(moodTag, reactionTag));
        var availablePairs = pairs.stream()
                .filter(pair -> !recentTagPairs.contains(tagPairKey(pair.get(0), pair.get(1)))).toList();
        if (availablePairs.isEmpty()) availablePairs = pairs;
        var pair = availablePairs.get(random.nextInt(availablePairs.size()));
        // Retain the original middle tag slot so V2 concepts remain compatible with stored history.
        var listeningTags = sorted(m.tags().get(1)).stream().filter(tag -> !pair.contains(tag)).toList();
        List<String> tags = List.of(pair.get(0), listeningTags.get(random.nextInt(listeningTags.size())), pair.get(1));
        return new CharacterConcept(2, "v2", "v2", d.version(), seed, profile,
                selected.expression(), selected.pose(), selected.prop(), selected.marks(), nameId, tags);
    }

    private static List<String> sorted(List<String> values) { return values.stream().sorted().toList(); }

    private static String tagPairKey(String first, String second) {
        return List.of(first.replaceFirst("^#", "").toLowerCase(Locale.ROOT),
                        second.replaceFirst("^#", "").toLowerCase(Locale.ROOT))
                .stream().sorted().collect(java.util.stream.Collectors.joining("/"));
    }

    private static long mixSeed(long seed) {
        long mixed = (seed ^ (seed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }
}
