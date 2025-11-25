package com.quitmate.user.users.repository;

import com.quitmate.user.users.entity.enums.UserSearchCategory;
import com.quitmate.user.users.entity.enums.UserSortType;
import com.quitmate.user.users.repository.response.UserDto;
import com.quitmate.user.users.service.request.UserListServiceRequest;
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

import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<UserDto> findUserList(UserListServiceRequest request, Pageable pageable) {
        List<UserDto> content = jpaQueryFactory
                .select(Projections.constructor(UserDto.class,
                        user.id,
                        user.createdDate,
                        user.email,
                        user.nickName,
                        user.birthDay,
                        user.sex
                ))
                .from(user)
                .where(
                        searchCondition(request.getCategory(), request.getKeyword())
//                        ,user.useYn.eq("Y")
                )
                .orderBy(getOrderSpecifier(request.getSortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(request);

        return new PageImpl<>(content, pageable, total);
    }

    private long getTotalCount(UserListServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(user.count())
                        .from(user)
                        .where(
                                searchCondition(request.getCategory(), request.getKeyword())
//                                ,user.useYn.eq("Y")
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    private BooleanExpression searchCondition(UserSearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case EMAIL -> user.email.containsIgnoreCase(keyword);
            case NICK_NAME -> user.nickName.containsIgnoreCase(keyword);
            case CREATED_DATE -> user.createdDate.stringValue().contains(keyword);
        };
    }

    private OrderSpecifier<?> getOrderSpecifier(UserSortType sortBy) {
        if (sortBy == UserSortType.NICK_NAME) {
            return user.nickName.asc();
        }
        // 기본값: 가입일자 내림차순
        return user.createdDate.desc();
    }
}
