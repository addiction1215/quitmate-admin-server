package com.quitmate.challenge.challange.repository;

import com.addiction.challenge.challange.controller.request.AdminChallengeSearchRequest;
import com.addiction.challenge.challange.repository.response.AdminChallengeDto;
import com.addiction.challenge.challange.repository.response.ChallengeDto;
import com.addiction.challenge.challange.repository.response.QAdminChallengeDto;
import com.addiction.challenge.challengehistory.entity.ChallengeStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.addiction.challenge.challange.entity.QChallenge.challenge;
import static com.addiction.challenge.challengehistory.entity.QChallengeHistory.challengeHistory;
import static com.addiction.challenge.mission.entity.QMission.mission;
import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
@Repository
public class ChallengeQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<ChallengeDto> findByUserId(Long userId) {
        JPAQuery<ChallengeDto> query = jpaQueryFactory
                .select(Projections.constructor(ChallengeDto.class,
                        challenge.id,
                        challenge.title,
                        challenge.content,
                        challenge.badge,
                        challengeHistory.status.coalesce(ChallengeStatus.PROGRESSING)
                ))
                .from(challenge)
                .leftJoin(challengeHistory)
                .on(challenge.id.eq(challengeHistory.challenge.id)
                        .and(challengeHistory.user.id.eq(userId)));

        return query.fetch();
    }

    /**
     * 진행중인 챌린지 1개 조회 (최신순, LIMIT 1)
     */
    public Optional<ChallengeDto> findProgressingChallengeByUserId(Long userId) {
        ChallengeDto result = jpaQueryFactory
                .select(Projections.constructor(ChallengeDto.class,
                        challenge.id,
                        challenge.title,
                        challenge.content,
                        challenge.badge,
                        challengeHistory.status
                ))
                .from(challengeHistory)
                .innerJoin(challenge).on(challengeHistory.challenge.id.eq(challenge.id))
                .where(
                        challengeHistory.user.id.eq(userId),
                        challengeHistory.status.eq(ChallengeStatus.PROGRESSING)
                )
                .orderBy(challengeHistory.createdDate.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 특정 상태의 챌린지 목록 조회 (페이징 지원)
     */
    public Page<ChallengeDto> findByUserIdAndStatus(Long userId, ChallengeStatus status, Pageable pageable) {
        List<ChallengeDto> content = jpaQueryFactory
                .select(Projections.constructor(ChallengeDto.class,
                        challenge.id,
                        challenge.title,
                        challenge.content,
                        challenge.badge,
                        challengeHistory.status
                ))
                .from(challengeHistory)
                .innerJoin(challenge).on(challengeHistory.challenge.id.eq(challenge.id))
                .where(
                        challengeHistory.user.id.eq(userId),
                        challengeHistory.status.eq(status)
                )
                .orderBy(challengeHistory.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, getTotalChallengeCount(userId, status));
    }

    public Long getTotalChallengeCount(Long userId, ChallengeStatus status) {
        return ofNullable(
                jpaQueryFactory
                        .select(challengeHistory.count())
                        .from(challengeHistory)
                        .where(
                                challengeHistory.user.id.eq(userId),
                                challengeHistory.status.eq(status)
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    /**
     * Admin용 챌린지 목록 조회 (검색, 정렬, 페이징 지원)
     */
    public Page<AdminChallengeDto> findAllForAdmin(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword,
            Pageable pageable) {
        
        List<AdminChallengeDto> content = jpaQueryFactory
                .select(new QAdminChallengeDto(
                        challenge.id,
                        challenge.title,
                        challenge.badge,
                        challenge.reward,
                        mission.count(),
                        challengeHistory.count()
                ))
                .from(challenge)
                .leftJoin(mission).on(mission.challenge.id.eq(challenge.id))
                .leftJoin(challengeHistory).on(challengeHistory.challenge.id.eq(challenge.id))
                .where(searchCondition(category, keyword))
                .groupBy(challenge.id, challenge.title, challenge.badge, challenge.reward)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = getTotalAdminChallengeCount(category, keyword);
        
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Admin용 챌린지 전체 개수 조회
     */
    private Long getTotalAdminChallengeCount(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword) {
        return ofNullable(
                jpaQueryFactory
                        .select(challenge.countDistinct())
                        .from(challenge)
                        .where(searchCondition(category, keyword))
                        .fetchOne()
        ).orElse(0L);
    }

    /**
     * 검색 조건 생성
     */
    private BooleanExpression searchCondition(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        if (category == null) {
            return null;
        }

        return switch (category) {
            case TITLE -> challenge.title.containsIgnoreCase(keyword);
            case BADGE -> challenge.badge.containsIgnoreCase(keyword);
            case REWARD -> {
                try {
                    Integer rewardValue = Integer.parseInt(keyword);
                    yield challenge.reward.eq(rewardValue);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }
}
