package com.quitmate.challenge.metrics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지표 기간. 참여 시작일(challenge_history.created_date, 한국 날짜) 기준으로 from·to 날짜를 모두 포함한다.
 * 둘 다 없어도 되고 하나만 있어도 된다.
 *
 * @param from 시작일(포함). null이면 처음부터
 * @param to   끝 날짜(포함). null이면 지금까지
 */
public record ChallengeMetricsPeriod(LocalDate from, LocalDate to) {

    public static final ChallengeMetricsPeriod ALL = new ChallengeMetricsPeriod(null, null);

    /** 이 시각 이상(from 00:00). null이면 아래쪽 경계 없음 */
    public LocalDateTime startInclusive() {
        return from == null ? null : from.atStartOfDay();
    }

    /** 이 시각 미만(to 다음 날 00:00). null이면 위쪽 경계 없음 */
    public LocalDateTime endExclusive() {
        return to == null ? null : to.plusDays(1).atStartOfDay();
    }

    public boolean contains(LocalDateTime startedAt) {
        if (startedAt == null) {
            return from == null && to == null;
        }
        LocalDateTime start = startInclusive();
        LocalDateTime end = endExclusive();
        return (start == null || !startedAt.isBefore(start)) && (end == null || startedAt.isBefore(end));
    }
}
