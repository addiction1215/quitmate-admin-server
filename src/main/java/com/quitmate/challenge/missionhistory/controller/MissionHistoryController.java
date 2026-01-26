package com.quitmate.challenge.missionhistory.controller;

import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryListRequest;
import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryUpdateRequest;
import com.quitmate.challenge.missionhistory.service.MissionHistoryReadService;
import com.quitmate.challenge.missionhistory.service.MissionHistoryService;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryDetailResponse;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/mission-history")
public class MissionHistoryController {

    private final MissionHistoryReadService missionHistoryReadService;
    private final MissionHistoryService missionHistoryService;

    /**
     * 미션 히스토리 목록 조회 API (완료 요청)
     */
    @GetMapping
    public ApiResponse<PageCustom<MissionHistoryListResponse>> getMissionHistoryList(
            @Valid @ModelAttribute MissionHistoryListRequest request) {
        return ApiResponse.ok(missionHistoryReadService.getMissionHistoryList(request.toServiceRequest()));
    }

    /**
     * 미션 히스토리 상세 조회 API
     */
    @GetMapping("/{id}")
    public ApiResponse<MissionHistoryDetailResponse> getMissionHistoryDetail(
            @PathVariable Long id) {
        return ApiResponse.ok(missionHistoryReadService.getMissionHistoryDetail(id));
    }

    /**
     * 미션 결과 심사 API (성공/실패 처리)
     */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMissionResult(
            @PathVariable Long id,
            @Valid @RequestBody MissionHistoryUpdateRequest request) {
        missionHistoryService.updateMissionResult(id, request.toServiceRequest());
    }
}
