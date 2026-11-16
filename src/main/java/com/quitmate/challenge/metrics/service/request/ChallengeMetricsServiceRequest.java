package com.quitmate.challenge.metrics.service.request;

import lombok.Builder;
import lombok.Getter;

/**
 * 지표 기간. 형식 검사는 서비스가 한다(잘못되면 사람이 읽는 문장으로 400).
 *
 * @see com.quitmate.challenge.metrics.service.ChallengeMetricsPeriod
 */
@Getter
public class ChallengeMetricsServiceRequest {

    private final String from;
    private final String to;

    @Builder
    public ChallengeMetricsServiceRequest(String from, String to) {
        this.from = from;
        this.to = to;
    }
}
