package com.quitmate.user.userPass.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.repository.response.UserPassDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.quitmate.user.userPass.entity.QUserPass.userPass;
import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class UserPassQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /** 최근 부여한 것부터. 회원 이메일·닉네임을 함께 준다(탈퇴 등으로 회원이 없으면 null). */
    public Page<UserPassDto> findUserPassList(Long userId, UserPassStatus status, LocalDateTime now,
                                              Pageable pageable) {
        List<UserPassDto> content = jpaQueryFactory
                .select(Projections.constructor(UserPassDto.class,
                        userPass.id,
                        userPass.userId,
                        user.email,
                        user.nickName,
                        userPass.product,
                        userPass.source,
                        userPass.startsAt,
                        userPass.endsAt,
                        userPass.revokedAt,
                        userPass.memo,
                        userPass.createdDate
                ))
                .from(userPass)
                .leftJoin(user).on(user.id.eq(userPass.userId))
                .where(userIdEq(userId), statusEq(status, now))
                .orderBy(userPass.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = ofNullable(jpaQueryFactory
                .select(userPass.count())
                .from(userPass)
                .where(userIdEq(userId), statusEq(status, now))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : userPass.userId.eq(userId);
    }

    /** {@link UserPassStatus#of}와 같은 기준 */
    private BooleanExpression statusEq(UserPassStatus status, LocalDateTime now) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case REVOKED -> userPass.revokedAt.isNotNull();
            case EXPIRED -> userPass.revokedAt.isNull().and(userPass.endsAt.loe(now));
            case SCHEDULED -> userPass.revokedAt.isNull().and(userPass.endsAt.gt(now)).and(userPass.startsAt.gt(now));
            case ACTIVE -> userPass.revokedAt.isNull().and(userPass.endsAt.gt(now)).and(userPass.startsAt.loe(now));
        };
    }
}
