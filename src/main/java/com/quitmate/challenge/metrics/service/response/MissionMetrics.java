package com.quitmate.challenge.metrics.service.response;

import com.quitmate.challenge.mission.entity.MissionCategoryStatus;

/**
 * 미션 하나의 완료율(KAN-194). 앱 서버는 참여를 시작할 때 그때 사용 중인 미션마다 미션 기록(mission_history)을
 * 하나씩 만든다. 그래서 분모는 미션마다 다르다(참여 뒤에 추가한 미션은 그 참여에 기록이 없음).
 *
 * @param missionId  미션 ID
 * @param title      미션 제목
 * @param category   미션 종류
 * @param completion 완료율 = 완료(COMPLETED)한 미션 기록 ÷ 기간 안에 시작한 참여의 이 미션 기록. 참여를 포기했어도 분모에 든다.
 *                   KAN-197 정리 전 옛 승인 대기(READY) 기록은 완료로 세지 않는다
 */
public record MissionMetrics(
        Long missionId,
        String title,
        MissionCategoryStatus category,
        MetricRatio completion
) {
}
