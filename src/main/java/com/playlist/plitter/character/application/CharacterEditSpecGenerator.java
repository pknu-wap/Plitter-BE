package com.playlist.plitter.character.application;

import com.playlist.plitter.character.application.dto.CharacterConcept;
import com.playlist.plitter.character.application.port.dto.CharacterEditSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CharacterEditSpecGenerator {
    private final CharacterConceptCatalog catalog;

    public CharacterEditSpec generate(CharacterConcept concept) {
        var d = catalog.data();
        if (!d.version().equals(concept.catalogVersion()))
            throw new IllegalArgumentException("Unsupported concept catalog");
        String prompt = """
                Create one music-themed variation of the input doodle star mascot.
                Preserve the simple five-point star body, tiny minimal face, thin stick arms and legs,
                tiny hands and feet, awkward uneven asymmetry, and rough naive hand-drawn identity.
                Music genre: %s. Mood group: %s.
                Apply this exact planned concept:
                - Expression: %s
                - Pose: %s
                - Sole main prop or accessory: %s
                - Surrounding marks: %s
                Keep one full-body character. Keep the face visible and the star silhouette recognizable.
                Use at most one main prop/accessory, smaller than half the star body.
                An attached accessory must be smaller than the face area. Add no more than three tiny marks.
                Keep thin wobbly uneven black lines and small naive irregularities.
                Use a fully transparent alpha background. No words, letters, labels or captions in the image.
                No colors, shading, gradients, 3D, paper texture, solid black filled areas or complex clothing.
                Do not polish, vectorize, thicken, or replace the star with another species or object.
                """.formatted(concept.profile().genre(), concept.profile().mood(),
                d.expressions().get(concept.expressionId()).directive(),
                d.poses().get(concept.poseId()).directive(),
                d.props().get(concept.propId()).directive(),
                d.marks().get(concept.marksId()).directive());
        return new CharacterEditSpec(prompt, concept.profile().mood().name());
    }
}
