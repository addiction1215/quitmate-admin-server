package com.quitmate.challenge.metrics.service.response;

/**
 * 챌린지 하나의 참여 지표(KAN-194). 대상은 기간 안에 시작한 참여(challenge_history.created_date, 한국 날짜)이고,
 * 상태는 지금 상태다. 기간을 안 주면 전체 참여다.
 *
 * <p>참여 1건 = challenge_history 1행. 앱 서버는 같은 챌린지를 진행 중·완료면 다시 참여하지 못하게 막아,
 * 한 사람이 같은 챌린지에 2건 이상 참여하는 것은 포기(CANCELLED) 뒤 다시 참여할 때뿐이다.
 *
 * @param challengeId            챌린지 ID
 * @param title                  챌린지 제목
 * @param participantCount       참여자 수(명). 기간 안에 참여를 시작한 서로 다른 사용자 수
 * @param participationCount     참여 수(건). 기간 안에 시작한 참여 수 = 아래 네 상태 수의 합
 * @param progressingCount       그중 지금 진행 중(PROGRESSING)인 참여 수
 * @param completedCount         그중 완료(COMPLETED)한 참여 수
 * @param cancelledCount         그중 포기(CANCELLED)한 참여 수
 * @param otherStatusCount       그중 다른 상태(FAILED 등. 지금은 어느 서버도 만들지 않아 보통 0)인 참여 수
 * @param completion             완료율 = completedCount ÷ participationCount
 * @param cancellation           중도 포기율 = cancelledCount ÷ participationCount
 * @param rejoin                 재참여율 = 포기 뒤 다시 참여한 사용자 ÷ 기간 안에 시작한 참여를 포기한 사용자(명).
 *                               다시 참여는 그 포기한 참여보다 뒤에 같은 챌린지에 참여를 시작한 것(기간 밖이어도, 지금 상태는 무관)
 * @param averageHoursToComplete 완료한 참여의 시작 → 완료까지 평균 시간(시간, 소수 한 자리). 완료가 없으면 null
 * @param averageHoursToCancel   포기한 참여의 시작 → 포기까지 평균 시간(시간, 소수 한 자리). 포기가 없으면 null
 * @param endTimeFallbackCount   위 두 평균에 쓴 완료·포기 중 완료·포기 시각(completed_at·cancelled_at)이 비어
 *                               마지막 수정 시각(updated_date)으로 대신한 건수(KAN-190 배포 전에 끝난 옛 기록)
 */
public record ChallengeMetrics(
        Long challengeId,
        String title,
        long participantCount,
        long participationCount,
        long progressingCount,
        long completedCount,
        long cancelledCount,
        long otherStatusCount,
        MetricRatio completion,
        MetricRatio cancellation,
        MetricRatio rejoin,
        Double averageHoursToComplete,
        Double averageHoursToCancel,
        long endTimeFallbackCount
) {
}
