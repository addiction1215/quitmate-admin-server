package com.quitmate.smokingPattern.service.response;

import java.time.LocalDate;

/**
 * 한 주의 전체 지표 분포. 유료는 그 주와 겹치는(회수되지 않은) QuitMate 90 이용권이 있는 사용자, 무료는 나머지다.
 *
 * @param periodStart 분석 주 월요일. 스냅숏이 하나도 없으면 null
 * @param periodEnd   분석 주 일요일. 스냅숏이 하나도 없으면 null
 */
public record SmokingPatternDistributionResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        SmokingPatternDistribution all,
        SmokingPatternDistribution paid,
        SmokingPatternDistribution free
) {
}
