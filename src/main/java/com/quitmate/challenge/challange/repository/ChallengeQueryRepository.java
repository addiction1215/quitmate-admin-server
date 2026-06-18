package com.quitmate.challenge.challange.repository;

import com.quitmate.challenge.challange.controller.request.SearchCategory;
import com.quitmate.challenge.challange.controller.request.SortType;
import com.quitmate.challenge.challange.repository.response.ChallengeDto;
import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.quitmate.challenge.challange.entity.QChallenge.challenge;
import static com.quitmate.challenge.challengehistory.entity.QChallengeHistory.challengeHistory;
import static com.quitmate.challenge.mission.entity.QMission.mission;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class ChallengeQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * Admin용 챌린지 목록 조회 (검색, 정렬, 페이징 지원)
     */
    public Page<ChallengeDto> findChallengeList(ChallengeSearchServiceRequest request, Pageable pageable) {
        List<ChallengeDto> content = jpaQueryFactory
                .select(Projections.constructor(ChallengeDto.class,
                        challenge.id,
                        challenge.title,
                        challenge.badge,
                        challenge.reward,
                        mission.count(),
                        challengeHistory.count()
                ))
                .from(challenge)
                .leftJoin(mission).on(mission.challenge.id.eq(challenge.id), mission.useYn.eq("Y"))
                .leftJoin(challengeHistory).on(challengeHistory.challenge.id.eq(challenge.id))
                .where(
                        challenge.useYn.eq("Y"),
                        searchCondition(request.getCategory(), request.getKeyword())
                )
                .groupBy(challenge.id, challenge.title, challenge.badge, challenge.reward)
                .orderBy(getOrderSpecifier(request.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(request);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Admin용 챌린지 전체 개수 조회
     */
    private long getTotalCount(ChallengeSearchServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(challenge.countDistinct())
                        .from(challenge)
                        .where(
                                challenge.useYn.eq("Y"),
                                searchCondition(request.getCategory(), request.getKeyword())
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    /**
     * 검색 조건 생성
     */
    private BooleanExpression searchCondition(SearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case TITLE -> challenge.title.containsIgnoreCase(keyword);
            case BADGE -> challenge.badge.containsIgnoreCase(keyword);
            case REWARD -> rewardCondition(keyword);
        };
    }

    /**
     * 보상 검색 조건
     */
    private BooleanExpression rewardCondition(String keyword) {
        try {
            Integer rewardValue = Integer.parseInt(keyword);
            return challenge.reward.eq(rewardValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 정렬 조건 생성
     */
    private OrderSpecifier<?> getOrderSpecifier(SortType sortType) {
        if (sortType == SortType.ID) {
            return challenge.id.asc();
        } else if (sortType == SortType.USER) {
            return challengeHistory.count().desc();
        }
        // 기본값: ID 내림차순
        return challenge.id.desc();
    }
}
