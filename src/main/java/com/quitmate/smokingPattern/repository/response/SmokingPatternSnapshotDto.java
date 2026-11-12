package com.quitmate.smokingPattern.repository.response;

import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스냅숏 목록 한 줄. 무거운 result_json은 읽지 않는다. 회원 정보는 닉네임만(회원이 없으면 null).
 */
public record SmokingPatternSnapshotDto(
        Long id,
        Long userId,
        String nickName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String algorithmVersion,
        DataSufficiencyLevel dataSufficiency,
        int observedDays,
        int smokeCount,
        Integer peakHour,
        RoutineType routineType,
        TrendDirection trendDirection,
        LocalDateTime createdDate
) {
}
