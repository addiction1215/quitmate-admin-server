package com.quitmate.challenge.missionhistory.controller;

import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryListRequest;
import com.quitmate.challenge.missionhistory.service.MissionHistoryReadService;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryDetailResponse;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 미션 제출 기록 조회. 관리자 승인·반려(PATCH /{id})는 KAN-190·194에서 없앴다.
 * 이제 앱 서버가 조건을 채운 제출을 바로 완료로 바꾸고, 관리자는 사진·위치·시간을 보기만 한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/mission-history")
public class MissionHistoryController {

    private final MissionHistoryReadService missionHistoryReadService;

    /**
     * 미션 히스토리 목록 조회 API
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
}
