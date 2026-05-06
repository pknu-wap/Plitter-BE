package com.playlist.plitter.track.presentation;

import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.track.application.TrackService;
import com.playlist.plitter.track.application.dto.TrackSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackService trackService;

    @GetMapping("/search")
    public ResponseDto<List<TrackSearchResponse>> searchTracks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<TrackSearchResponse> result = trackService.searchTracks(keyword, limit);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, result);
    }
}