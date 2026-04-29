package com.playlist.plitter.track.domain.entity;

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
@Table(name = "tracks")
public class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "spotify_track_id", nullable = false, length = 100)
    private String spotifyTrackId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "album_name", length = 200)
    private String albumName;

    @Column(name = "album_cover_url", columnDefinition = "TEXT")
    private String albumCoverUrl;

    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    @Column(name = "spotify_url", columnDefinition = "TEXT")
    private String spotifyUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TrackEntity(
            String spotifyTrackId,
            String title,
            String artistName,
            String albumName,
            String albumCoverUrl,
            String previewUrl,
            String spotifyUrl
    ) {
        this.spotifyTrackId = spotifyTrackId;
        this.title = title;
        this.artistName = artistName;
        this.albumName = albumName;
        this.albumCoverUrl = albumCoverUrl;
        this.previewUrl = previewUrl;
        this.spotifyUrl = spotifyUrl;
    }
}