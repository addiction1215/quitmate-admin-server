package com.quitmate.smokingPattern.controller;

import com.quitmate.global.ApiResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.smokingPattern.controller.request.SmokingPatternSnapshotListRequest;
import com.quitmate.smokingPattern.controller.request.SmokingPatternUserSnapshotListRequest;
import com.quitmate.smokingPattern.service.SmokingPatternReadService;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistributionResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotDetailResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotSummaryResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternWeekResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 어드민 「흡연 패턴 진단」 화면(KAN-141·142). 관리자 계정만 쓸 수 있다(서비스에서 DB로 확인).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/smoking-pattern")
public class SmokingPatternController {

    private final SmokingPatternReadService smokingPatternReadService;

    /** 스냅숏이 있는 분석 주(최근 주부터, 기본 12주) */
    @GetMapping("/weeks")
    public ApiResponse<List<SmokingPatternWeekResponse>> getWeeks(
            @RequestParam(name = "size", required = false) Integer size) {
        return ApiResponse.ok(smokingPatternReadService.getWeeks(size));
    }

    /** 한 주의 사용자별 스냅숏 목록 */
    @GetMapping("/snapshots")
    public ApiResponse<PageCustom<SmokingPatternSnapshotSummaryResponse>> getWeekSnapshots(
            @Valid @ModelAttribute SmokingPatternSnapshotListRequest request) {
        return ApiResponse.ok(smokingPatternReadService.getWeekSnapshots(request.toServiceRequest()));
    }

    /** 스냅숏 한 건의 지표 1~9 상세 */
    @GetMapping("/snapshots/{snapshotId}")
    public ApiResponse<SmokingPatternSnapshotDetailResponse> getSnapshot(@PathVariable("snapshotId") Long snapshotId) {
        return ApiResponse.ok(smokingPatternReadService.getSnapshot(snapshotId));
    }

    /** 한 회원의 주차별 스냅숏 목록 */
    @GetMapping("/users/{userId}/snapshots")
    public ApiResponse<PageCustom<SmokingPatternSnapshotSummaryResponse>> getUserSnapshots(
            @PathVariable("userId") Long userId,
            @Valid @ModelAttribute SmokingPatternUserSnapshotListRequest request) {
        return ApiResponse.ok(smokingPatternReadService.getUserSnapshots(userId, request.toServiceRequest()));
    }

    /** 한 주의 전체·유료·무료 지표 분포 */
    @GetMapping("/distribution")
    public ApiResponse<SmokingPatternDistributionResponse> getDistribution(
            @RequestParam(name = "periodStart", required = false) String periodStart) {
        return ApiResponse.ok(smokingPatternReadService.getDistribution(periodStart));
    }
}
