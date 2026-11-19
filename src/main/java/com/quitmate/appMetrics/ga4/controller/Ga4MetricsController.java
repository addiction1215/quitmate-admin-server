package com.quitmate.appMetrics.ga4.controller;

import com.quitmate.appMetrics.ga4.service.Ga4MetricsService;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 「앱 지표」 화면의 GA4 섹션(KAN-218). 관리자 계정만 쓸 수 있다(SecurityConfig, KAN-206).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app-metrics")
public class Ga4MetricsController {

    private final Ga4MetricsService ga4MetricsService;

    /**
     * GA4 활성 사용자·화면 조회 수·플랫폼·앱 버전. period는 7·28·90(그 밖은 400).
     * 연결 전이면 200 + enabled=false, GA4 호출 실패면 503 + message GA4_UNAVAILABLE.
     * period는 숫자가 아닐 때도 400이 되도록 글자로 받아 서비스에서 확인한다.
     */
    @GetMapping("/ga4")
    public ApiResponse<Ga4MetricsResponse> getGa4Metrics(
            @RequestParam(name = "period", required = false) String period) {
        return ApiResponse.ok(ga4MetricsService.getMetrics(period));
    }
}
