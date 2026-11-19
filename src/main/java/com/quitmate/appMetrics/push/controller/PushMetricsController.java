package com.quitmate.appMetrics.push.controller;

import com.quitmate.appMetrics.push.service.PushMetricsService;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse;
import com.quitmate.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 「앱 지표」 화면의 푸시 섹션(KAN-221). 관리자 계정만 쓸 수 있다(SecurityConfig, KAN-206).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app-metrics")
public class PushMetricsController {

    private final PushMetricsService pushMetricsService;

    /**
     * 푸시 발송 요청·전달(Expo 티켓·영수증)·열림·토큰 보유 회원 수. period는 7·28·90(그 밖은 400).
     * period는 숫자가 아닐 때도 400이 되도록 글자로 받아 서비스에서 확인한다.
     */
    @GetMapping("/push")
    public ApiResponse<PushMetricsResponse> getPushMetrics(
            @RequestParam(name = "period", required = false) String period) {
        return ApiResponse.ok(pushMetricsService.getMetrics(period));
    }
}
