package com.playlist.plitter.character.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.character.application.dto.MusicProfile.Mood;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
public class CharacterConceptCatalog {
    public record Option(String directive, int weight) {}
    public record MoodOptions(List<String> expressions, List<String> poses, List<String> marks,
                              List<String> names, List<List<String>> tags) {}
    public record Catalog(String version, Map<String, MoodOptions> moods, Map<String, List<String>> genres,
                          Map<String, Option> expressions, Map<String, Option> poses,
                          Map<String, Option> props, Map<String, Option> marks) {}

    private final Catalog data;

    public CharacterConceptCatalog() {
        try (InputStream input = new ClassPathResource("character/character-concepts-v2.json").getInputStream()) {
            data = new ObjectMapper().readValue(input, Catalog.class);
            validate();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid character concept catalog", e);
        }
    }

    public Catalog data() { return data; }
    public MoodOptions mood(Mood mood) { return data.moods().get(mood.name()); }
    public List<String> props(String genre) { return data.genres().getOrDefault(genre, data.genres().get("OTHER")); }
    public String name(Mood mood, String id) {
        String prefix = mood.name() + ":";
        if (!id.startsWith(prefix)) throw new IllegalArgumentException("Name does not match mood");
        return mood(mood).names().get(Integer.parseInt(id.substring(prefix.length())));
    }

    private void validate() {
        if (!"v2".equals(data.version()) || !data.genres().containsKey("OTHER"))
            throw new IllegalArgumentException("Unsupported catalog version");
        for (Map<String, Option> options : List.of(data.expressions(), data.poses(), data.props(), data.marks())) {
            options.forEach((id, option) -> {
                if (id.isBlank() || option.directive() == null || option.directive().isBlank()
                        || option.weight() < 1 || option.weight() > 100)
                    throw new IllegalArgumentException("Invalid option: " + id);
            });
        }
        for (Mood mood : Mood.values()) {
            MoodOptions m = Objects.requireNonNull(mood(mood), "Missing mood " + mood);
            checkIds(m.expressions(), data.expressions(), 3);
            checkIds(m.poses(), data.poses(), 4);
            checkIds(m.marks(), data.marks(), 3);
            if (m.names().size() < 6 || new HashSet<>(m.names()).size() != m.names().size())
                throw new IllegalArgumentException("Need distinct names: " + mood);
            for (String name : m.names())
                if (!name.endsWith(" 스타") || name.length() > 18)
                    throw new IllegalArgumentException("Invalid name: " + name);
            if (m.tags().size() != 3 || m.tags().stream().anyMatch(t -> t.size() < 3))
                throw new IllegalArgumentException("Need three tag pools");
            Set<String> allTags = new HashSet<>();
            for (List<String> pool : m.tags()) for (String tag : pool) {
                if (tag.isBlank() || tag.length() > 11 || tag.startsWith("#")
                        || tag.chars().anyMatch(Character::isWhitespace) || !allTags.add(tag))
                    throw new IllegalArgumentException("Invalid or duplicate tag: " + tag);
            }
            for (List<String> props : data.genres().values()) {
                checkIds(props, data.props(), 2);
                if ((long)m.expressions().size() * m.poses().size() * m.marks().size() * props.size() < 24)
                    throw new IllegalArgumentException("Too few combinations");
            }
        }
    }

    private static void checkIds(List<String> ids, Map<String, Option> options, int minimum) {
        if (ids.size() < minimum || new HashSet<>(ids).size() != ids.size()
                || !options.keySet().containsAll(ids)) throw new IllegalArgumentException("Invalid option references");
    }
}
