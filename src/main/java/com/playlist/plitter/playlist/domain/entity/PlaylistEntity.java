package com.playlist.plitter.playlist.domain.entity;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "playlist")
public class PlaylistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playlist_id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false, unique = true)
    private UserEntity owner;

    @Column(name = "short_id", nullable = false, length = 20, unique = true)
    private String shortId;

    @Builder.Default
    @Column(name = "recommendation_count", nullable = false)
    private int recommendationCount = 0;

    @Column(name = "character_version", nullable = false)
    private int characterVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PlaylistEntity(UserEntity owner, String shortId, int characterVersion) {
        this.owner = owner;
        this.shortId = shortId;
        this.characterVersion = characterVersion;
    }
}