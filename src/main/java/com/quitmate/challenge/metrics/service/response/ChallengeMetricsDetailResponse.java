package com.quitmate.challenge.metrics.service.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 챌린지 하나의 참여 지표와 미션별 완료율.
 *
 * @param from      기간 시작일(참여 시작일 기준, 이 날 포함). 안 주면 null(처음부터)
 * @param to        기간 끝 날짜(참여 시작일 기준, 이 날 포함). 안 주면 null(지금까지)
 * @param challenge 챌린지 지표
 * @param missions  사용 중인 미션별 완료율(미션 ID 순)
 */
public record ChallengeMetricsDetailResponse(
        LocalDate from,
        LocalDate to,
        ChallengeMetrics challenge,
        List<MissionMetrics> missions
) {
}
