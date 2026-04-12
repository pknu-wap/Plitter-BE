package com.playlist.plitter.playlist.application;

import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.exception.PlaylistErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    public PlaylistCreateResponse savePlaylist(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (playlistRepository.existsByOwner(user)) {
            throw new ApiException(PlaylistErrorCode.PLAYLIST_ALREADY_EXISTS);
        }

        String shortId = UUID.randomUUID().toString().substring(0, 5);

        PlaylistEntity playlist = PlaylistEntity.builder()
                .owner(user)
                .shortId(shortId)
                .characterVersion(0)
                .build();

        PlaylistEntity saved = playlistRepository.save(playlist);

        Long playlistId = saved.getId();
        String shareUrl = "https://ourdomain.com/playlist/" + saved.getShortId();

        return new PlaylistCreateResponse(playlistId, shortId, shareUrl);
    }

}
