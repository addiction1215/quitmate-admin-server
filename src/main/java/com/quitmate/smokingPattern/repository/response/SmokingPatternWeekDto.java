package com.quitmate.smokingPattern.repository.response;

import java.time.LocalDate;

/** 스냅숏이 있는 분석 주와 그 주 스냅숏 수 */
public record SmokingPatternWeekDto(LocalDate periodStart, LocalDate periodEnd, long snapshotCount) {
}
