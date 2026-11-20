package com.quitmate.challenge.metrics.repository;

import com.quitmate.challenge.metrics.repository.response.ChallengeTitle;
import com.quitmate.challenge.metrics.repository.response.MissionStatusCount;
import com.quitmate.challenge.metrics.repository.response.ParticipationRow;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.quitmate.challenge.challange.entity.QChallenge.challenge;
import static com.quitmate.challenge.challengehistory.entity.QChallengeHistory.challengeHistory;
import static com.quitmate.challenge.missionhistory.entity.QMissionHistory.missionHistory;

/**
 * 챌린지 지표(KAN-194)용 읽기 쿼리. 앱 서버가 쓰는 challenge_history·mission_history를 읽기만 한다.
 */
@Repository
@RequiredArgsConstructor
public class ChallengeMetricsQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /** 사용 중인 챌린지(최근 챌린지부터) */
    public List<ChallengeTitle> findActiveChallenges() {
        return jpaQueryFactory
                .select(Projections.constructor(ChallengeTitle.class, challenge.id, challenge.title))
                .from(challenge)
                .where(challenge.useYn.eq("Y"))
                .orderBy(challenge.id.desc())
                .fetch();
    }

    /**
     * 주어진 챌린지들의 참여를 참여 ID 순서로 {@code afterId} 다음부터 {@code limit}건. 기간으로 거르지 않는다
     * (포기 뒤 다시 참여했는지를 보려면 기간 밖 참여도 필요하다).
     */
    public List<ParticipationRow> findParticipationsAfter(Collection<Long> challengeIds, long afterId, int limit) {
        if (challengeIds.isEmpty()) {
            return List.of();
        }
        return jpaQueryFactory
                .select(Projections.constructor(ParticipationRow.class,
                        challengeHistory.id,
                        challengeHistory.challenge.id,
                        challengeHistory.user.id,
                        challengeHistory.status,
                        challengeHistory.createdDate,
                        challengeHistory.updatedDate,
                        challengeHistory.completedAt,
                        challengeHistory.cancelledAt))
                .from(challengeHistory)
                .where(
                        challengeHistory.challenge.id.in(challengeIds),
                        challengeHistory.id.gt(afterId)
                )
                .orderBy(challengeHistory.id.asc())
                .limit(limit)
                .fetch();
    }

    /**
     * 한 챌린지에서 기간 안에 시작한 참여의 미션 기록을 미션·상태별로 센다.
     *
     * @param startInclusive 참여 시작 이 시각 이상. null이면 경계 없음
     * @param endExclusive   참여 시작 이 시각 미만. null이면 경계 없음
     */
    public List<MissionStatusCount> countMissionHistories(Long challengeId, LocalDateTime startInclusive,
                                                          LocalDateTime endExclusive) {
        return jpaQueryFactory
                .select(Projections.constructor(MissionStatusCount.class,
                        missionHistory.mission.id,
                        missionHistory.status,
                        missionHistory.count()))
                .from(missionHistory)
                .join(missionHistory.challengeHistory, challengeHistory)
                .where(
                        challengeHistory.challenge.id.eq(challengeId),
                        startedFrom(startInclusive),
                        startedBefore(endExclusive)
                )
                .groupBy(missionHistory.mission.id, missionHistory.status)
                .fetch();
    }

    private static BooleanExpression startedFrom(LocalDateTime startInclusive) {
        return startInclusive == null ? null : challengeHistory.createdDate.goe(startInclusive);
    }

    private static BooleanExpression startedBefore(LocalDateTime endExclusive) {
        return endExclusive == null ? null : challengeHistory.createdDate.lt(endExclusive);
    }
}
