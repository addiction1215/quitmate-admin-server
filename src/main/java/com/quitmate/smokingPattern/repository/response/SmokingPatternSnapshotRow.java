package com.quitmate.smokingPattern.repository.response;

import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.result.SmokingPatternResult;

/**
 * 전체 분포를 계산할 때 한 주의 스냅숏을 나눠 읽는 한 줄. 엔티티로 읽지 않아 영속성 컨텍스트에 쌓이지 않는다.
 *
 * @param result 지표 1~9 전체. JSON을 읽지 못했으면 null
 */
public record SmokingPatternSnapshotRow(
        Long id,
        Long userId,
        String algorithmVersion,
        DataSufficiencyLevel dataSufficiency,
        int observedDays,
        int smokeCount,
        Integer peakHour,
        RoutineType routineType,
        TrendDirection trendDirection,
        SmokingPatternResult result
) {
}
