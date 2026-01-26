package com.quitmate.challenge.missionhistory.repository;

import com.quitmate.challenge.missionhistory.controller.request.MissionHistorySearchCategory;
import com.quitmate.challenge.missionhistory.controller.request.MissionHistorySortType;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.challenge.missionhistory.repository.response.MissionHistoryDto;
import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
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
import static com.quitmate.challenge.missionhistory.entity.QMissionHistory.missionHistory;
import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class MissionHistoryQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<MissionHistoryDto> findMissionHistoryList(
            MissionHistoryListServiceRequest request, Pageable pageable) {

        List<MissionHistoryDto> content = jpaQueryFactory
                .select(Projections.constructor(MissionHistoryDto.class,
                        missionHistory.id,
                        challenge.title,
                        mission.title,
                        mission.category,
                        missionHistory.status,
                        user.nickName,
                        missionHistory.createdDate
                ))
                .from(missionHistory)
                .join(missionHistory.mission, mission)
                .join(mission.challenge, challenge)
                .join(missionHistory.challengeHistory, challengeHistory)
                .join(missionHistory.user, user)
                .where(
                        missionHistory.status.eq(MissionStatus.READY),
                        searchCondition(request.getCategory(), request.getKeyword())
                )
                .orderBy(getOrderSpecifier(request.getSortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(request);

        return new PageImpl<>(content, pageable, total);
    }

    private long getTotalCount(MissionHistoryListServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(missionHistory.count())
                        .from(missionHistory)
                        .join(missionHistory.mission, mission)
                        .join(mission.challenge, challenge)
                        .join(missionHistory.challengeHistory, challengeHistory)
                        .join(missionHistory.user, user)
                        .where(
                                missionHistory.status.eq(MissionStatus.READY),
                                searchCondition(request.getCategory(), request.getKeyword())
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    private BooleanExpression searchCondition(MissionHistorySearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case CHALLENGE_TITLE -> challenge.title.containsIgnoreCase(keyword);
            case MISSION_TITLE -> mission.title.containsIgnoreCase(keyword);
            case USER_NAME -> user.nickName.containsIgnoreCase(keyword);
            case STATUS -> statusCondition(keyword);
        };
    }

    private BooleanExpression statusCondition(String keyword) {
        if ("대기".equals(keyword) || "READY".equalsIgnoreCase(keyword)) {
            return missionHistory.status.eq(MissionStatus.READY);
        } else if ("완료".equals(keyword) || "COMPLETED".equalsIgnoreCase(keyword)) {
            return missionHistory.status.eq(MissionStatus.COMPLETED);
        } else if ("실패".equals(keyword) || "FAILED".equalsIgnoreCase(keyword)) {
            return missionHistory.status.eq(MissionStatus.FAILED);
        }
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(MissionHistorySortType sortBy) {
        if (sortBy == MissionHistorySortType.STATUS) {
            return missionHistory.status.asc();
        }
        // 기본값: 요청일시 내림차순 (최신순)
        return missionHistory.createdDate.desc();
    }
}
