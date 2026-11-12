package com.quitmate.smokingPattern.service.response;

import com.quitmate.smokingPattern.repository.response.SmokingPatternWeekDto;

import java.time.LocalDate;

/** 스냅숏이 있는 분석 주(월~일)와 그 주 스냅숏 수 */
public record SmokingPatternWeekResponse(LocalDate periodStart, LocalDate periodEnd, long snapshotCount) {

    public static SmokingPatternWeekResponse of(SmokingPatternWeekDto dto) {
        return new SmokingPatternWeekResponse(dto.periodStart(), dto.periodEnd(), dto.snapshotCount());
    }
}
