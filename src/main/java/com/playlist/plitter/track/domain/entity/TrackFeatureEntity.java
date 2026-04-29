package com.playlist.plitter.track.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "track_features")
public class TrackFeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false, unique = true)
    private TrackEntity track;

    @Column(name = "bpm", precision = 6, scale = 2)
    private BigDecimal bpm;

    @Column(name = "mood", length = 100)
    private String mood;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "energy", precision = 5, scale = 2)
    private BigDecimal energy;

    @Column(name = "valence", precision = 5, scale = 2)
    private BigDecimal valence;

    @Column(name = "raw_feature_json", columnDefinition = "jsonb")
    private String rawFeatureJson;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TrackFeatureEntity(
            TrackEntity track,
            BigDecimal bpm,
            String mood,
            String genre,
            BigDecimal energy,
            BigDecimal valence,
            String rawFeatureJson,
            LocalDateTime fetchedAt
    ) {
        this.track = track;
        this.bpm = bpm;
        this.mood = mood;
        this.genre = genre;
        this.energy = energy;
        this.valence = valence;
        this.rawFeatureJson = rawFeatureJson;
        this.fetchedAt = fetchedAt;
    }
}