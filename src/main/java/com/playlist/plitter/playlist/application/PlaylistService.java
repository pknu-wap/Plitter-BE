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
        System.out.println("서비스 시작 !!");

        System.out.println("레포지토리 접근 시작 !!");
        PlaylistEntity saved = playlistRepository.save(playlist);
        System.out.println("레포지토리 접근 종료 !!");

        Long playlistId = saved.getId();
        String shortId = UUID.randomUUID().toString().substring(0, 5);
        String shareUrl = "temp"; // 🧐 수정 필요

        System.out.println("서비스 종료 !!");
        return new PlaylistCreateResponse(playlistId, shortId, shareUrl);
    }

}
