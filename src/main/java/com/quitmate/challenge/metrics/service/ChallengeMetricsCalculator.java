package com.quitmate.challenge.metrics.service;

import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.metrics.repository.response.ParticipationRow;
import com.quitmate.challenge.metrics.service.response.ChallengeMetrics;
import com.quitmate.challenge.metrics.service.response.MetricRatio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 챌린지 하나의 참여를 한 줄씩 받아 지표를 더한다(DB·시계 없음).
 *
 * <p>재참여를 알아보려고 기간과 상관없이 그 챌린지의 모든 참여를 <b>참여 ID 순서로</b> 받는다. 지표 대상(분모)은
 * 기간 안에 시작한 참여뿐이고, 기간 밖 참여는 "포기 뒤 다시 참여했는지"를 볼 때만 쓴다.
 */
public class ChallengeMetricsCalculator {

    private static final double SECONDS_PER_TENTH_HOUR = 360.0;

    private final ChallengeMetricsPeriod period;

    private long participationCount;
    private long progressingCount;
    private long completedCount;
    private long cancelledCount;
    private long otherStatusCount;
    private final Set<Long> participants = new HashSet<>();

    /** 기간 안에 시작한 참여를 포기한 사용자 */
    private final Set<Long> cancelledUsers = new HashSet<>();
    /** 그 포기 뒤 같은 챌린지에 다시 참여를 시작한 사용자 */
    private final Set<Long> rejoinedUsers = new HashSet<>();

    private long completeSeconds;
    private long completeDurations;
    private long cancelSeconds;
    private long cancelDurations;
    private long endTimeFallbackCount;

    private Long lastId;

    public ChallengeMetricsCalculator(ChallengeMetricsPeriod period) {
        this.period = period;
    }

    public void add(ParticipationRow row) {
        if (lastId != null && row.id() <= lastId) {
            throw new IllegalStateException("참여는 ID 순서로 넣어야 합니다.");
        }
        lastId = row.id();

        Long userId = row.userId();
        // 앞서 기간 안 참여를 포기한 사람이 이 참여를 시작했으면 다시 참여한 것이다(이 참여가 기간 밖이어도)
        if (userId != null && cancelledUsers.contains(userId)) {
            rejoinedUsers.add(userId);
        }
        if (!period.contains(row.startedAt())) {
            return;
        }

        participationCount++;
        if (userId != null) {
            participants.add(userId);
        }
        ChallengeStatus status = row.status();
        if (status == ChallengeStatus.PROGRESSING) {
            progressingCount++;
        } else if (status == ChallengeStatus.COMPLETED) {
            completedCount++;
            addCompleteDuration(row);
        } else if (status == ChallengeStatus.CANCELLED) {
            cancelledCount++;
            if (userId != null) {
                cancelledUsers.add(userId);
            }
            addCancelDuration(row);
        } else {
            otherStatusCount++;
        }
    }

    public ChallengeMetrics result(Long challengeId, String title) {
        return new ChallengeMetrics(
                challengeId,
                title,
                participants.size(),
                participationCount,
                progressingCount,
                completedCount,
                cancelledCount,
                otherStatusCount,
                MetricRatio.of(completedCount, participationCount),
                MetricRatio.of(cancelledCount, participationCount),
                MetricRatio.of(rejoinedUsers.size(), cancelledUsers.size()),
                averageHours(completeSeconds, completeDurations),
                averageHours(cancelSeconds, cancelDurations),
                endTimeFallbackCount
        );
    }

    private void addCompleteDuration(ParticipationRow row) {
        Long seconds = durationSeconds(row.startedAt(), row.completedAt(), row.updatedAt());
        if (seconds != null) {
            completeSeconds += seconds;
            completeDurations++;
        }
    }

    private void addCancelDuration(ParticipationRow row) {
        Long seconds = durationSeconds(row.startedAt(), row.cancelledAt(), row.updatedAt());
        if (seconds != null) {
            cancelSeconds += seconds;
            cancelDurations++;
        }
    }

    /**
     * 시작부터 끝까지 초. 끝 시각이 없으면(KAN-190 전 옛 행) 마지막 수정 시각으로 대신하고 그 수를 센다.
     * 시각을 모르거나 끝이 시작보다 앞서면(잘못된 데이터) 평균에서 뺀다.
     */
    private Long durationSeconds(LocalDateTime startedAt, LocalDateTime endedAt, LocalDateTime updatedAt) {
        boolean fallback = endedAt == null;
        LocalDateTime end = fallback ? updatedAt : endedAt;
        if (startedAt == null || end == null || end.isBefore(startedAt)) {
            return null;
        }
        if (fallback) {
            endTimeFallbackCount++;
        }
        return Duration.between(startedAt, end).getSeconds();
    }

    private static Double averageHours(long totalSeconds, long count) {
        if (count == 0) {
            return null;
        }
        return Math.round(totalSeconds / (double) count / SECONDS_PER_TENTH_HOUR) / 10.0;
    }
}
