package com.quitmate.challenge.missionhistory.controller;

import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryListRequest;
import com.quitmate.challenge.missionhistory.service.MissionHistoryReadService;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/mission-history")
public class MissionHistoryController {

    private final MissionHistoryReadService missionHistoryReadService;

    /**
     * 미션 히스토리 목록 조회 API (완료 요청)
     */
    @GetMapping
    public ApiResponse<PageCustom<MissionHistoryListResponse>> getMissionHistoryList(
            @Valid @ModelAttribute MissionHistoryListRequest request) {
        return ApiResponse.ok(missionHistoryReadService.getMissionHistoryList(request.toServiceRequest()));
    }
}
