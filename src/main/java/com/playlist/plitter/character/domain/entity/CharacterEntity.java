package com.playlist.plitter.character.domain.entity;

import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "characters")
public class CharacterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "feature_summary_json", columnDefinition = "jsonb")
    private String featureSummaryJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CharacterEntity(
            PlaylistEntity playlist,
            int version,
            String imageUrl,
            String promptText,
            String featureSummaryJson
    ) {
        this.playlist = playlist;
        this.version = version;
        this.imageUrl = imageUrl;
        this.promptText = promptText;
        this.featureSummaryJson = featureSummaryJson;
    }

    public static CharacterEntity create(
            PlaylistEntity playlist,
            int version,
            String imageUrl,
            String promptText,
            String featureSummaryJson
    ) {
        return new CharacterEntity(playlist, version, imageUrl, promptText, featureSummaryJson);
    }
}
