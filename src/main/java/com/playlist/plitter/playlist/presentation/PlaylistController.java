package com.playlist.plitter.playlist.presentation;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.playlist.application.PlaylistService;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import com.playlist.plitter.playlist.exception.PlaylistErrorCode;
import lombok.RequiredArgsConstructor;
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
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;

    @PostMapping("/playlists")
    public ResponseDto<PlaylistCreateResponse> createPlaylist(
            @RequestHeader("Authorization") String token
        ) {
        Long userId = 1L; // TODO: JWT 토큰에서 userId 추출

        PlaylistCreateResponse response = playlistService.savePlaylist(userId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }
}
