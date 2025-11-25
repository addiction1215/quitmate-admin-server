package com.quitmate.challenge.rewardHistory.controller;

import com.quitmate.challenge.rewardHistory.controller.request.RewardHistoryListRequest;
import com.quitmate.challenge.rewardHistory.service.RewardHistoryReadService;
import com.quitmate.challenge.rewardHistory.service.response.RewardHistoryListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reward-histories")
public class RewardHistoryController {

    private final RewardHistoryReadService rewardHistoryReadService;

    @GetMapping
    public ApiResponse<PageCustom<RewardHistoryListResponse>> getRewardHistoryList(
            @Valid @ModelAttribute RewardHistoryListRequest request) {
        return ApiResponse.ok(rewardHistoryReadService.getRewardHistoryList(request.toServiceRequest()));
    }
}
