package com.quitmate.faq.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.service.request.FaqListServiceRequest;
import com.quitmate.faq.service.response.FaqListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.quitmate.faq.entity.QFaq.faq;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class FaqQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<FaqListResponse> findFaqList(FaqListServiceRequest request, Pageable pageable) {
        List<FaqListResponse> content = jpaQueryFactory
                .select(Projections.constructor(FaqListResponse.class,
                        faq.id,
                        faq.category,
                        faq.pinned,
                        faq.sortOrder,
                        faq.title,
                        faq.description
                ))
                .from(faq)
                .where(
                        faq.useYn.eq("Y"),
                        categoryCondition(request.getCategory()),
                        keywordCondition(request.getKeyword())
                )
                .orderBy(
                        faq.pinned.desc(),
                        faq.sortOrder.asc(),
                        faq.createdDate.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = ofNullable(
                jpaQueryFactory
                        .select(faq.count())
                        .from(faq)
                        .where(
                                faq.useYn.eq("Y"),
                                categoryCondition(request.getCategory()),
                                keywordCondition(request.getKeyword())
                        )
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression categoryCondition(FaqCategory category) {
        if (category == null) {
            return null;
        }
        return faq.category.eq(category);
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return faq.title.containsIgnoreCase(keyword);
    }
}
