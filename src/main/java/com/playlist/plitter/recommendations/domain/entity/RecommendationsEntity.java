package com.playlist.plitter.recommendations.domain.entity;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.track.domain.entity.TrackEntity;
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
@Table(name = "recommendations")
public class RecommendationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private TrackEntity track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommender_user_id")
    private UserEntity recommenderUser;

    @Column(name = "guest_token", length = 100)
    private String guestToken;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(name = "comment", nullable = false, length = 300)
    private String comment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RecommendationsEntity(
            PlaylistEntity playlist,
            TrackEntity track,
            UserEntity recommenderUser,
            String guestToken,
            boolean isAnonymous,
            String comment
    ) {
        this.playlist = playlist;
        this.track = track;
        this.recommenderUser = recommenderUser;
        this.guestToken = guestToken;
        this.isAnonymous = isAnonymous;
        this.comment = comment;
    }
}