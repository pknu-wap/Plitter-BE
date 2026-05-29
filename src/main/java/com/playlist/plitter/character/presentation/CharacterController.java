package com.playlist.plitter.character.presentation;

import com.playlist.plitter.character.application.CharacterService;
import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists/{playlistId}/character")
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping("/availability")
    public ResponseDto<CharacterAvailabilityResponse> getCharacterAvailability(
            @PathVariable Long playlistId,
            @AuthenticationPrincipal Long requesterUserId
    ) {
        CharacterAvailabilityResponse response = characterService.getAvailability(playlistId, requesterUserId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @PostMapping
    public ResponseDto<CharacterCreateResponse> createCharacter(
            @PathVariable Long playlistId,
            @AuthenticationPrincipal Long requesterUserId
    ) {
        CharacterCreateResponse response = characterService.createCharacter(playlistId, requesterUserId);
        return ResponseDto.ofSuccess(SuccessMessage.CREATE_SUCCESS, response);
    }

    @GetMapping
    public ResponseDto<CharacterDetailResponse> getCharacter(
            @PathVariable Long playlistId,
            @AuthenticationPrincipal Long requesterUserId
    ) {
        CharacterDetailResponse response = characterService.getCharacter(playlistId, requesterUserId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/download-url")
    public ResponseDto<CharacterDownloadUrlResponse> getCharacterDownloadUrl(
            @PathVariable Long playlistId,
            @AuthenticationPrincipal Long requesterUserId
    ) {
        CharacterDownloadUrlResponse response = characterService.getCharacterDownloadUrl(playlistId, requesterUserId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }
}
