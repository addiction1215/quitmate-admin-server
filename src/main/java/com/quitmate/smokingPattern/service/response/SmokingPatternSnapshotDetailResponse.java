package com.quitmate.smokingPattern.service.response;

import com.quitmate.smokingPattern.entity.SmokingPatternSnapshot;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.result.SmokingPatternResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스냅숏 한 건의 지표 1~9 전체(어드민 전용 지표 7·8 포함).
 *
 * @param nickName  회원이 없으면 null
 * @param paid      그 주와 겹치는(회수되지 않은) QuitMate 90 이용권이 있었는지
 * @param result    지표 1~9 전체. result_json을 읽지 못했으면 null
 * @param updatedDate 같은 주를 다시 계산해 고쳐 쓴 시각
 */
public record SmokingPatternSnapshotDetailResponse(
        Long id,
        Long userId,
        String nickName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String algorithmVersion,
        DataSufficiencyLevel dataSufficiency,
        int observedDays,
        int smokeCount,
        boolean paid,
        SmokingPatternResult result,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {

    public static SmokingPatternSnapshotDetailResponse of(SmokingPatternSnapshot snapshot, String nickName, boolean paid) {
        return new SmokingPatternSnapshotDetailResponse(snapshot.getId(), snapshot.getUserId(), nickName,
                snapshot.getPeriodStart(), snapshot.getPeriodEnd(), snapshot.getAlgorithmVersion(),
                snapshot.getDataSufficiency(), snapshot.getObservedDays(), snapshot.getSmokeCount(), paid,
                snapshot.getResult(), snapshot.getCreatedDate(), snapshot.getUpdatedDate());
    }
}
