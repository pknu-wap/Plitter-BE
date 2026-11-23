package com.playlist.plitter.character.application.dto;

import java.util.List;

public record CharacterConcept(
        int schemaVersion, String ruleVersion, String promptVersion, String catalogVersion,
        long seed, MusicProfile profile, String expressionId, String poseId,
        String propId, String marksId, String nameId, List<String> tagIds
) {
    public CharacterConcept {
        tagIds = List.copyOf(tagIds);
    }

    public String signature() {
        return String.join("/", expressionId, poseId, propId, marksId);
    }
}
