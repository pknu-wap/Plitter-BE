package com.playlist.plitter.recommendations.presentation;

import com.playlist.plitter.global.dto.ResponseDto;
import com.playlist.plitter.global.dto.SuccessMessage;
import com.playlist.plitter.recommendations.application.RecommendationsService;
import com.playlist.plitter.recommendations.application.dto.RecommendationCreateRequest;
import com.playlist.plitter.recommendations.application.dto.RecommendationCreateResponse;
import com.playlist.plitter.recommendations.application.dto.RecommendationDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class RecommendationsController {

    private final RecommendationsService recommendationsService;

    @PostMapping("/{playlistId}/recommendations")
    public ResponseDto<RecommendationCreateResponse> createRecommendation(
            @PathVariable Long playlistId,
            @RequestBody RecommendationCreateRequest request
    ) {
        RecommendationCreateResponse response = recommendationsService.createRecommendation(playlistId, request);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }

    @GetMapping("/recommendations/{recommendationId}")
    public ResponseDto<RecommendationDetailResponse> getRecommendationDetail(
            @PathVariable Long recommendationId
    ) {
        RecommendationDetailResponse response = recommendationsService.getRecommendationDetail(recommendationId);
        return ResponseDto.ofSuccess(SuccessMessage.OPERATION_SUCCESS, response);
    }
}
