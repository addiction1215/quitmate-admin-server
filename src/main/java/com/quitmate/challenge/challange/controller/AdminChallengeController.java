package com.quitmate.challenge.challange.controller;

import com.addiction.challenge.challange.controller.request.AdminChallengeSearchRequest;
import com.addiction.challenge.challange.service.AdminChallengeService;
import com.addiction.challenge.challange.service.response.AdminChallengeListResponse;
import com.addiction.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/challenge")
public class AdminChallengeController {
    
    private final AdminChallengeService adminChallengeService;
    
    @GetMapping
    public ApiResponse<Page<AdminChallengeListResponse>> getChallengeList(
            @ModelAttribute AdminChallengeSearchRequest request) {
        Page<AdminChallengeListResponse> response = adminChallengeService.getChallengeList(
                request.toServiceRequest()
        );
        return ApiResponse.ok(response);
    }
}
