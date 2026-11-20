package com.quitmate.challenge.metrics.repository.response;

import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;

import java.time.LocalDateTime;

/**
 * 지표 계산에 쓰는 챌린지 참여(challenge_history) 한 행. 시각은 모두 한국 시각이다.
 *
 * @param id          참여 ID(앞선 참여일수록 작다)
 * @param challengeId 챌린지 ID
 * @param userId      사용자 ID
 * @param status      지금 상태
 * @param startedAt   참여 시작(created_date)
 * @param updatedAt   마지막 수정(updated_date). 완료·포기 시각이 없는 옛 행은 이것으로 대신한다
 * @param completedAt 완료 시각(KAN-190 배포 뒤 완료만 있음)
 * @param cancelledAt 포기 시각(KAN-190 배포 뒤 포기만 있음)
 */
public record ParticipationRow(
        Long id,
        Long challengeId,
        Long userId,
        ChallengeStatus status,
        LocalDateTime startedAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt
) {
}
