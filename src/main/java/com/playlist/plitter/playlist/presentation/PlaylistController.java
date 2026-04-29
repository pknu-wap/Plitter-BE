package com.playlist.plitter.playlist.presentation;

import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.playlist.application.PlaylistService;
import com.playlist.plitter.playlist.application.dto.PlaylistCheckResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.application.dto.PlaylistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping("/playlists")
    public ResponseDto<PlaylistCreateResponse> createPlaylist(
            @RequestHeader("Authorization") String token
        ) {
        Long userId = 1L; // TODO: JWT 토큰에서 userId 추출

        PlaylistCreateResponse response = playlistService.savePlaylist(userId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/playlists/check")
    public ResponseDto<PlaylistCheckResponse> checkPlaylist(
            @RequestHeader("Authorization") String token
        ) {
        Long userId = 1L; // TODO: JWT 토큰에서 userId 추출

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
