package com.quitmate.challenge.metrics.controller;

import com.quitmate.challenge.metrics.controller.request.ChallengeMetricsRequest;
import com.quitmate.challenge.metrics.service.ChallengeMetricsReadService;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsDetailResponse;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsListResponse;
import com.quitmate.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 챌린지 지표(KAN-194): 참여자 수·완료율·중도 포기율·재참여율·미션별 완료율. 관리자 계정만 쓸 수 있다(서비스에서 DB로 확인).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/challenge")
public class ChallengeMetricsController {

    private final ChallengeMetricsReadService challengeMetricsReadService;

    /** 사용 중인 챌린지 전체의 지표(챌린지 목록 화면용) */
    @GetMapping("/metrics")
    public ApiResponse<ChallengeMetricsListResponse> getChallengeMetricsList(
            @ModelAttribute ChallengeMetricsRequest request) {
        return ApiResponse.ok(challengeMetricsReadService.getChallengeMetricsList(request.toServiceRequest()));
    }

    /** 챌린지 하나의 지표와 미션별 완료율(챌린지 상세 화면용) */
    @GetMapping("/{id}/metrics")
    public ApiResponse<ChallengeMetricsDetailResponse> getChallengeMetrics(
            @PathVariable("id") Long id,
            @ModelAttribute ChallengeMetricsRequest request) {
        return ApiResponse.ok(challengeMetricsReadService.getChallengeMetrics(id, request.toServiceRequest()));
    }
}
