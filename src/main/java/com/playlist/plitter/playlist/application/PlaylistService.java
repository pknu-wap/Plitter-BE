package com.playlist.plitter.playlist.application;

import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlaylistService {

    @Autowired
    PlaylistRepository playlistRepository;

    public PlaylistCreateResponse savePlaylist(PlaylistEntity playlist) {

        PlaylistEntity saved = playlistRepository.save(playlist);

        Long playlistId = saved.getId();
        String shortId = UUID.randomUUID().toString().substring(0, 5);
        String shareUrl = "https://ourdomain.com/playlist/" + shortId;

        return new PlaylistCreateResponse(playlistId, shortId, shareUrl);
    }

}
