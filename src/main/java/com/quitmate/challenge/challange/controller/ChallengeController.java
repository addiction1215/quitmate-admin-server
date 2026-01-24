package com.quitmate.challenge.challange.controller;

import com.quitmate.challenge.challange.controller.request.ChallengeSearchRequest;
import com.quitmate.challenge.challange.service.ChallengeReadService;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/challenge")
public class ChallengeController {

    private final ChallengeReadService challengeReadService;

    @GetMapping
    public ApiResponse<PageCustom<ChallengeListResponse>> getChallengeList(
            @ModelAttribute ChallengeSearchRequest request) {
        return ApiResponse.ok(challengeReadService.getChallengeList(
                request.toServiceRequest()
        ));
    }
}
