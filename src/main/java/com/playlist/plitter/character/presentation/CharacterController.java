package com.playlist.plitter.character.presentation;

import com.playlist.plitter.character.application.CharacterService;
import com.playlist.plitter.character.presentation.dto.response.CharacterAvailabilityResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterCreateResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDetailResponse;
import com.playlist.plitter.character.presentation.dto.response.CharacterDownloadUrlResponse;
import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists/{playlistId}/character")
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping("/availability")
    public ResponseDto<CharacterAvailabilityResponse> getCharacterAvailability(
            @PathVariable Long playlistId
    ) {
        CharacterAvailabilityResponse response = characterService.getAvailability(playlistId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @PostMapping
    public ResponseDto<CharacterCreateResponse> createCharacter(
            @PathVariable Long playlistId
    ) {
        CharacterCreateResponse response = characterService.createCharacter(playlistId);
        return ResponseDto.ofSuccess(SuccessMessage.CREATE_SUCCESS, response);
    }

    @GetMapping
    public ResponseDto<CharacterDetailResponse> getCharacter(
            @PathVariable Long playlistId
    ) {
        CharacterDetailResponse response = characterService.getCharacter(playlistId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/download-url")
    public ResponseDto<CharacterDownloadUrlResponse> getCharacterDownloadUrl(
            @PathVariable Long playlistId
    ) {
        CharacterDownloadUrlResponse response = characterService.getCharacterDownloadUrl(playlistId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }
}
