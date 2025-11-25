package com.quitmate.notice.repository;

import com.quitmate.notice.enums.NoticeSearchCategory;
import com.quitmate.notice.enums.NoticeSortType;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.response.NoticeDto;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
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

import static com.quitmate.notice.entity.QNotice.notice;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class NoticeQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<NoticeDto> findNoticeList(NoticeListServiceRequest request, Pageable pageable) {
        List<NoticeDto> content = jpaQueryFactory
                .select(Projections.constructor(NoticeDto.class,
                        notice.id,
                        notice.type,
                        notice.content,
                        notice.createdDate
                ))
                .from(notice)
                .where(
                        searchCondition(request.getCategory(), request.getKeyword()),
                        notice.useYn.eq("Y")
                )
                .orderBy(getOrderSpecifier(request.getSortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = getTotalCount(request);

        return new PageImpl<>(content, pageable, total);
    }

    private long getTotalCount(NoticeListServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(notice.count())
                        .from(notice)
                        .where(
                                searchCondition(request.getCategory(), request.getKeyword()),
                                notice.useYn.eq("Y")
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    private BooleanExpression searchCondition(NoticeSearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case CONTENT -> notice.content.containsIgnoreCase(keyword);
            case TYPE -> typeCondition(keyword);
            case CREATED_DATE -> notice.createdDate.stringValue().contains(keyword);
        };
    }

    private BooleanExpression typeCondition(String keyword) {
        if ("새 기능 추가".equals(keyword) || "NEW_FEATURE".equalsIgnoreCase(keyword)) {
            return notice.type.eq(NoticeType.NEW_FEATURE);
        } else if ("기존 기능 수정".equals(keyword) || "FEATURE_UPDATE".equalsIgnoreCase(keyword)) {
            return notice.type.eq(NoticeType.FEATURE_UPDATE);
        } else if ("이벤트".equals(keyword) || "EVENT".equalsIgnoreCase(keyword)) {
            return notice.type.eq(NoticeType.EVENT);
        } else if ("서비스 점검".equals(keyword) || "SERVICE_CHECK".equalsIgnoreCase(keyword)) {
            return notice.type.eq(NoticeType.SERVICE_CHECK);
        }
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(NoticeSortType sortBy) {
        if (sortBy == NoticeSortType.TYPE) {
            return notice.type.asc();
        }
        // 기본값: 작성일시 내림차순
        return notice.createdDate.desc();
    }
}
