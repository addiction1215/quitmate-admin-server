package com.quitmate.smokingPattern.service.response;

import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotDto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스냅숏 목록 한 줄. 개인정보는 닉네임만 준다(이메일·생년월일·성별 없음).
 *
 * @param nickName 회원이 없으면 null
 * @param paid     그 주와 겹치는(회수되지 않은) QuitMate 90 이용권이 있었는지
 */
public record SmokingPatternSnapshotSummaryResponse(
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
        boolean paid,
        LocalDateTime createdDate
) {

    public static SmokingPatternSnapshotSummaryResponse of(SmokingPatternSnapshotDto dto, boolean paid) {
        return new SmokingPatternSnapshotSummaryResponse(dto.id(), dto.userId(), dto.nickName(), dto.periodStart(),
                dto.periodEnd(), dto.algorithmVersion(), dto.dataSufficiency(), dto.observedDays(), dto.smokeCount(),
                dto.peakHour(), dto.routineType(), dto.trendDirection(), paid, dto.createdDate());
    }
}
