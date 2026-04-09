package com.playlist.plitter.playlist.presentation;

import com.playlist.plitter.auth.domain.entity.UserEntity;
import com.playlist.plitter.auth.domain.repository.UserRepository;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.playlist.application.PlaylistService;
import com.playlist.plitter.playlist.application.dto.PlaylistCreateResponse;
import com.playlist.plitter.playlist.domain.entity.PlaylistEntity;
import com.playlist.plitter.playlist.domain.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.h2.engine.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;

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
        Long userId = 1L; // 🧐 TODO: JWT 토큰에서 userId 추출

        System.out.println("user id = " + userId);
        System.out.println("✅ playlist 생성 컨트롤러 시작");

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        if (playlistRepository.existsByOwner(user)) {
            throw new PlaylistAlreadyExistsException();
        }

        String shortId = UUID.randomUUID().toString().substring(0, 8);

        PlaylistEntity playlist = PlaylistEntity.builder()
                .owner(user)
                .shortId(shortId)
                .characterVersion(1)
                .build();


        PlaylistCreateResponse response = playlistService.savePlaylist(playlist);
        System.out.println("playlist 생성 컨트롤러 종료");
        System.out.println("============================================");
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @ExceptionHandler(PlaylistAlreadyExistsException.class)
    public ResponseEntity<ResponseDto<Void>> handlePlaylistAlreadyExists() {
        System.out.println("PLAYLIST_ALREADY_EXISTS 예외 처리되어 종료");
        System.out.println("============================================");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseDto.ofFailure("PLAYLIST_ALREADY_EXISTS", "플레이리스트가 이미 존재합니다."));
    }

    static class PlaylistAlreadyExistsException extends RuntimeException {
    }
}
