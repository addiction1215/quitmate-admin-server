package com.quitmate.challenge.metrics.service;

import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.metrics.repository.response.ParticipationRow;
import com.quitmate.challenge.metrics.service.response.ChallengeMetrics;
import com.quitmate.challenge.metrics.service.response.MetricRatio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChallengeMetricsCalculatorTest {

    private static final LocalDateTime NOV_2 = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final ChallengeMetricsPeriod NOVEMBER_FIRST_WEEK =
            new ChallengeMetricsPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));

    private long nextId = 1;

    @DisplayName("참여가 없으면 수는 0이고 비율·평균은 null이다(분모 0)")
    @Test
    void empty() {
        ChallengeMetrics metrics = new ChallengeMetricsCalculator(ChallengeMetricsPeriod.ALL).result(1L, "빈 챌린지");

        assertThat(metrics.participantCount()).isZero();
        assertThat(metrics.participationCount()).isZero();
        assertThat(metrics.completion()).isEqualTo(new MetricRatio(0, 0, null));
        assertThat(metrics.cancellation()).isEqualTo(new MetricRatio(0, 0, null));
        assertThat(metrics.rejoin()).isEqualTo(new MetricRatio(0, 0, null));
        assertThat(metrics.averageHoursToComplete()).isNull();
        assertThat(metrics.averageHoursToCancel()).isNull();
        assertThat(metrics.endTimeFallbackCount()).isZero();
    }

    @DisplayName("완료율·포기율은 참여 건 기준, 참여자는 사람 기준이고 상태 수의 합은 참여 수와 같다")
    @Test
    void ratios() {
        // given: 1번은 포기 뒤 다시 참여해 2건
        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(ChallengeMetricsPeriod.ALL);
        calculator.add(cancelled(1L, NOV_2, NOV_2.plusHours(1)));
        calculator.add(completed(2L, NOV_2, NOV_2.plusHours(2)));
        calculator.add(completed(3L, NOV_2, NOV_2.plusHours(4)));
        calculator.add(progressing(1L, NOV_2.plusDays(1)));
        calculator.add(row(4L, ChallengeStatus.FAILED, NOV_2, NOV_2, null, null));
        calculator.add(progressing(5L, NOV_2));

        // when
        ChallengeMetrics metrics = calculator.result(7L, "출근길 금연");

        // then
        assertThat(metrics.challengeId()).isEqualTo(7L);
        assertThat(metrics.title()).isEqualTo("출근길 금연");
        assertThat(metrics.participantCount()).isEqualTo(5);
        assertThat(metrics.participationCount()).isEqualTo(6);
        assertThat(metrics.progressingCount() + metrics.completedCount() + metrics.cancelledCount()
                + metrics.otherStatusCount()).isEqualTo(metrics.participationCount());
        assertThat(metrics.otherStatusCount()).isEqualTo(1);
        assertThat(metrics.completion()).isEqualTo(new MetricRatio(2, 6, 33));
        assertThat(metrics.cancellation()).isEqualTo(new MetricRatio(1, 6, 17));
        assertThat(metrics.rejoin()).isEqualTo(new MetricRatio(1, 1, 100));
        assertThat(metrics.averageHoursToComplete()).isEqualTo(3.0);
        assertThat(metrics.averageHoursToCancel()).isEqualTo(1.0);
    }

    @DisplayName("재참여: 기간 안 참여를 포기한 사람 중 그 뒤(기간 밖이어도) 다시 참여한 사람. 포기 전 참여나 기간 전 포기는 세지 않는다")
    @Test
    void rejoin() {
        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(NOVEMBER_FIRST_WEEK);
        // 1번: 기간 전(10/20)에 포기 → 기간 안에 다시 참여. 분모(기간 안 포기)에 없다
        calculator.add(cancelled(1L, LocalDateTime.of(2026, 10, 20, 9, 0), LocalDateTime.of(2026, 10, 20, 10, 0)));
        // 2번: 기간 안에 완료만. 포기가 없어 분모에 없다
        calculator.add(completed(2L, NOV_2, NOV_2.plusHours(1)));
        // 3번: 기간 안에 포기 → 기간 뒤(11/20)에 다시 참여. 다시 참여로 센다
        calculator.add(cancelled(3L, NOV_2, NOV_2.plusMinutes(30)));
        // 4번: 기간 안에 포기만
        calculator.add(cancelled(4L, NOV_2, NOV_2.plusMinutes(90)));
        calculator.add(progressing(1L, LocalDateTime.of(2026, 11, 3, 9, 0)));
        calculator.add(progressing(3L, LocalDateTime.of(2026, 11, 20, 9, 0)));

        ChallengeMetrics metrics = calculator.result(1L, "챌린지");

        assertThat(metrics.participationCount()).as("10/20·11/20 참여는 기간 밖").isEqualTo(4);
        assertThat(metrics.participantCount()).isEqualTo(4);
        assertThat(metrics.rejoin()).isEqualTo(new MetricRatio(1, 2, 50));
        assertThat(metrics.averageHoursToCancel()).as("30분·90분 평균 1시간").isEqualTo(1.0);
    }

    @DisplayName("기간은 참여 시작일 기준으로 from 00:00 이상 · to 다음 날 00:00 미만")
    @Test
    void periodBoundary() {
        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(NOVEMBER_FIRST_WEEK);
        calculator.add(progressing(1L, LocalDateTime.of(2026, 10, 31, 23, 59, 59)));
        calculator.add(progressing(2L, LocalDateTime.of(2026, 11, 1, 0, 0)));
        calculator.add(progressing(3L, LocalDateTime.of(2026, 11, 7, 23, 59, 59)));
        calculator.add(progressing(4L, LocalDateTime.of(2026, 11, 8, 0, 0)));

        assertThat(calculator.result(1L, "챌린지").participationCount()).isEqualTo(2);
    }

    @DisplayName("완료·포기 시각이 비어 있는 옛 행은 마지막 수정 시각으로 대신하고 그 수를 센다. 끝이 시작보다 앞서면 평균에서 뺀다")
    @Test
    void endTimeFallback() {
        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(ChallengeMetricsPeriod.ALL);
        calculator.add(row(1L, ChallengeStatus.COMPLETED, NOV_2, NOV_2.plusHours(5), null, null));
        calculator.add(row(2L, ChallengeStatus.COMPLETED, NOV_2, NOV_2.plusDays(3), NOV_2.plusHours(1), null));
        calculator.add(row(3L, ChallengeStatus.CANCELLED, NOV_2, NOV_2.plusMinutes(15), null, null));
        calculator.add(row(4L, ChallengeStatus.CANCELLED, NOV_2, NOV_2.minusHours(1), null, null));

        ChallengeMetrics metrics = calculator.result(1L, "챌린지");

        assertThat(metrics.averageHoursToComplete()).as("5시간(옛 행)·1시간(완료 시각) 평균").isEqualTo(3.0);
        assertThat(metrics.averageHoursToCancel()).as("15분만, 시작보다 앞선 행은 뺌").isEqualTo(0.3);
        assertThat(metrics.endTimeFallbackCount()).isEqualTo(2);
        assertThat(metrics.cancelledCount()).as("상태 수에서는 빼지 않는다").isEqualTo(2);
    }

    @DisplayName("참여는 ID 순서로만 받는다(재참여 판단이 순서에 기댄다)")
    @Test
    void requiresIdOrder() {
        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(ChallengeMetricsPeriod.ALL);
        calculator.add(new ParticipationRow(5L, 1L, 1L, ChallengeStatus.PROGRESSING, NOV_2, NOV_2, null, null));

        assertThatThrownBy(() -> calculator.add(
                new ParticipationRow(5L, 1L, 2L, ChallengeStatus.PROGRESSING, NOV_2, NOV_2, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    private ParticipationRow progressing(Long userId, LocalDateTime startedAt) {
        return row(userId, ChallengeStatus.PROGRESSING, startedAt, startedAt, null, null);
    }

    private ParticipationRow completed(Long userId, LocalDateTime startedAt, LocalDateTime completedAt) {
        return row(userId, ChallengeStatus.COMPLETED, startedAt, completedAt, completedAt, null);
    }

    private ParticipationRow cancelled(Long userId, LocalDateTime startedAt, LocalDateTime cancelledAt) {
        return row(userId, ChallengeStatus.CANCELLED, startedAt, cancelledAt, null, cancelledAt);
    }

    private ParticipationRow row(Long userId, ChallengeStatus status, LocalDateTime startedAt, LocalDateTime updatedAt,
                                 LocalDateTime completedAt, LocalDateTime cancelledAt) {
        return new ParticipationRow(nextId++, 1L, userId, status, startedAt, updatedAt, completedAt, cancelledAt);
    }
}
