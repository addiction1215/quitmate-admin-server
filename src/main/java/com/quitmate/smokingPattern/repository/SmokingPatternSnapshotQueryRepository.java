package com.quitmate.smokingPattern.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.quitmate.smokingPattern.entity.QSmokingPatternSnapshot.smokingPatternSnapshot;
import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class SmokingPatternSnapshotQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * 한 주의 스냅숏 목록(회원 ID 순). period_start(+ data_sufficiency) 인덱스로 거른다.
     *
     * @param keyword 닉네임 또는 이메일 일부(선택). 응답에는 이메일을 넣지 않는다
     */
    public Page<SmokingPatternSnapshotDto> findWeekSnapshots(LocalDate periodStart, DataSufficiencyLevel dataSufficiency,
                                                             Long userId, String keyword, Pageable pageable) {
        BooleanExpression[] conditions = {
                smokingPatternSnapshot.periodStart.eq(periodStart),
                dataSufficiencyEq(dataSufficiency),
                userIdEq(userId),
                keywordContains(keyword)
        };

        List<SmokingPatternSnapshotDto> content = jpaQueryFactory
                .select(summary())
                .from(smokingPatternSnapshot)
                .leftJoin(user).on(user.id.eq(smokingPatternSnapshot.userId))
                .where(conditions)
                .orderBy(smokingPatternSnapshot.userId.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = ofNullable(jpaQueryFactory
                .select(smokingPatternSnapshot.count())
                .from(smokingPatternSnapshot)
                .leftJoin(user).on(user.id.eq(smokingPatternSnapshot.userId))
                .where(conditions)
                .fetchOne()).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    /** 한 회원의 주차별 스냅숏(최근 주부터). (user_id, period_start) 유니크 인덱스로 찾는다. */
    public Page<SmokingPatternSnapshotDto> findUserSnapshots(Long userId, Pageable pageable) {
        List<SmokingPatternSnapshotDto> content = jpaQueryFactory
                .select(summary())
                .from(smokingPatternSnapshot)
                .leftJoin(user).on(user.id.eq(smokingPatternSnapshot.userId))
                .where(smokingPatternSnapshot.userId.eq(userId))
                .orderBy(smokingPatternSnapshot.periodStart.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = ofNullable(jpaQueryFactory
                .select(smokingPatternSnapshot.count())
                .from(smokingPatternSnapshot)
                .where(smokingPatternSnapshot.userId.eq(userId))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private static com.querydsl.core.types.ConstructorExpression<SmokingPatternSnapshotDto> summary() {
        return Projections.constructor(SmokingPatternSnapshotDto.class,
                smokingPatternSnapshot.id,
                smokingPatternSnapshot.userId,
                user.nickName,
                smokingPatternSnapshot.periodStart,
                smokingPatternSnapshot.periodEnd,
                smokingPatternSnapshot.algorithmVersion,
                smokingPatternSnapshot.dataSufficiency,
                smokingPatternSnapshot.observedDays,
                smokingPatternSnapshot.smokeCount,
                smokingPatternSnapshot.peakHour,
                smokingPatternSnapshot.routineType,
                smokingPatternSnapshot.trendDirection,
                smokingPatternSnapshot.createdDate
        );
    }

    private BooleanExpression dataSufficiencyEq(DataSufficiencyLevel dataSufficiency) {
        return dataSufficiency == null ? null : smokingPatternSnapshot.dataSufficiency.eq(dataSufficiency);
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : smokingPatternSnapshot.userId.eq(userId);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        return user.nickName.containsIgnoreCase(trimmed).or(user.email.containsIgnoreCase(trimmed));
    }
}
