package com.quitmate.challenge.rewardHistory.repository;

import com.quitmate.challenge.rewardHistory.enums.RewardSearchCategory;
import com.quitmate.challenge.rewardHistory.enums.RewardSortType;
import com.quitmate.challenge.rewardHistory.enums.RewardType;
import com.quitmate.challenge.rewardHistory.repository.response.RewardHistoryDto;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
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

import static com.quitmate.challenge.rewardHistory.entity.QRewardHistory.rewardHistory;
import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class RewardHistoryQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<RewardHistoryDto> findRewardHistoryList(RewardHistoryListServiceRequest request, Pageable pageable) {
        List<RewardHistoryDto> content = jpaQueryFactory
                .select(Projections.constructor(RewardHistoryDto.class,
                        rewardHistory.id,
                        rewardHistory.createdDate,
                        user.nickName,
                        rewardHistory.type,
                        rewardHistory.point,
                        rewardHistory.remainingPoint
                ))
                .from(rewardHistory)
                .join(rewardHistory.user, user)
                .where(
                        searchCondition(request.getCategory(), request.getKeyword())
                )
                .orderBy(getOrderSpecifier(request.getSortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(request);

        return new PageImpl<>(content, pageable, total);
    }

    private long getTotalCount(RewardHistoryListServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(rewardHistory.count())
                        .from(rewardHistory)
                        .join(rewardHistory.user, user)
                        .where(
                                searchCondition(request.getCategory(), request.getKeyword())
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    private BooleanExpression searchCondition(RewardSearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case USER_NAME -> user.nickName.containsIgnoreCase(keyword);
            case TYPE -> typeCondition(keyword);
            case CREATED_DATE -> rewardHistory.createdDate.stringValue().contains(keyword);
        };
    }

    private BooleanExpression typeCondition(String keyword) {
        if ("사용".equals(keyword) || "USED".equalsIgnoreCase(keyword)) {
            return rewardHistory.type.eq(RewardType.USED);
        } else if ("획득".equals(keyword) || "적립".equals(keyword) || "ACQUIRE".equalsIgnoreCase(keyword)) {
            return rewardHistory.type.eq(RewardType.ACQUIRE);
        }
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(RewardSortType sortBy) {
        if (sortBy == RewardSortType.USER_NAME) {
            return user.nickName.asc();
        }
        // 기본값: 발생일자 내림차순
        return rewardHistory.createdDate.desc();
    }
}
