package com.quitmate.challenge.metrics.service.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 사용 중인 챌린지 전체의 참여 지표(최근 챌린지부터, ID 내림차순). 미션별 완료율은 상세에만 있다.
 *
 * @param from       기간 시작일(참여 시작일 기준, 이 날 포함). 안 주면 null(처음부터)
 * @param to         기간 끝 날짜(참여 시작일 기준, 이 날 포함). 안 주면 null(지금까지)
 * @param challenges 챌린지별 지표
 */
public record ChallengeMetricsListResponse(
        LocalDate from,
        LocalDate to,
        List<ChallengeMetrics> challenges
) {
}
