package com.quitmate.challenge.challange.controller;

import com.quitmate.challenge.challange.controller.request.ChallengeCreateRequest;
import com.quitmate.challenge.challange.controller.request.ChallengeSearchRequest;
import com.quitmate.challenge.challange.service.ChallengeReadService;
import com.quitmate.challenge.challange.service.ChallengeService;
import com.quitmate.challenge.challange.service.response.ChallengeCreateResponse;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/challenge")
public class ChallengeController {

    private final ChallengeReadService challengeReadService;
    private final ChallengeService challengeService;

    /**
     * 챌린지 목록 조회 API
     */
    @GetMapping
    public ApiResponse<PageCustom<ChallengeListResponse>> getChallengeList(
            @ModelAttribute ChallengeSearchRequest request) {
        return ApiResponse.ok(challengeReadService.getChallengeList(
                request.toServiceRequest()
        ));
    }

    /**
     * 챌린지 생성 API
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChallengeCreateResponse> createChallenge(
            @Valid @RequestBody ChallengeCreateRequest request) {
        return ApiResponse.created(challengeService.createChallenge(request.toServiceRequest()));
    }
}
