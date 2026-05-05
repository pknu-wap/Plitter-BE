package com.playlist.plitter.playlist.presentation;

import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.playlist.application.PlaylistService;
import com.playlist.plitter.playlist.application.dto.PlaylistCheckResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping("/playlists")
    public ResponseDto<PlaylistCreateResponse> createPlaylist(
            @AuthenticationPrincipal Long userId
        ) {
        PlaylistCreateResponse response = playlistService.savePlaylist(userId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/playlists/check")
    public ResponseDto<PlaylistCheckResponse> checkPlaylist(
            @AuthenticationPrincipal Long userId
        ) {
        PlaylistCheckResponse response = playlistService.checkPlaylist(userId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/playlists/{playlistId}")
    public ResponseDto<PlaylistResponse> getPlaylist(
            @PathVariable Long playlistId
    ) {
        PlaylistResponse response = playlistService.getPlaylist(playlistId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

}
