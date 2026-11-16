package com.quitmate.challenge.metrics.controller.request;

import com.quitmate.challenge.metrics.service.request.ChallengeMetricsServiceRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 지표 기간(참여 시작일 기준, 한국 날짜 yyyy-MM-dd, 두 날짜 모두 포함). 둘 다 선택이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChallengeMetricsRequest {

    private String from;
    private String to;

    @Builder
    public ChallengeMetricsRequest(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public ChallengeMetricsServiceRequest toServiceRequest() {
        return ChallengeMetricsServiceRequest.builder()
                .from(from)
                .to(to)
                .build();
    }
}
