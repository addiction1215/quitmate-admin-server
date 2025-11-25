package com.quitmate.inquiry.inquryQuestion.repository;

import com.quitmate.inquiry.inquryQuestion.enums.InquirySearchCategory;
import com.quitmate.inquiry.inquryQuestion.enums.InquirySortType;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.inquiry.inquryQuestion.repository.response.InquiryQuestionDto;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
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

import static com.quitmate.inquiry.inquryQuestion.entity.QInquiryQuestion.inquiryQuestion;
import static com.quitmate.user.users.entity.QUser.user;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class InquiryQuestionQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<InquiryQuestionDto> findInquiryQuestionList(InquiryQuestionListServiceRequest request, Pageable pageable) {
        List<InquiryQuestionDto> content = jpaQueryFactory
                .select(Projections.constructor(InquiryQuestionDto.class,
                        inquiryQuestion.id,
                        user.nickName,
                        inquiryQuestion.title,
                        inquiryQuestion.createdDate,
                        inquiryQuestion.inquiryStatus
                ))
                .from(inquiryQuestion)
                .join(inquiryQuestion.user, user)
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

    private long getTotalCount(InquiryQuestionListServiceRequest request) {
        return ofNullable(
                jpaQueryFactory
                        .select(inquiryQuestion.count())
                        .from(inquiryQuestion)
                        .join(inquiryQuestion.user, user)
                        .where(
                                searchCondition(request.getCategory(), request.getKeyword())
                        )
                        .fetchOne()
        ).orElse(0L);
    }

    private BooleanExpression searchCondition(InquirySearchCategory category, String keyword) {
        if (!StringUtils.hasText(keyword) || category == null) {
            return null;
        }

        return switch (category) {
            case TITLE -> inquiryQuestion.title.containsIgnoreCase(keyword);
            case STATUS -> statusCondition(keyword);
            case CREATED_DATE -> inquiryQuestion.createdDate.stringValue().contains(keyword);
        };
    }

    private BooleanExpression statusCondition(String keyword) {
        if ("대기".equals(keyword) || "WAITING".equalsIgnoreCase(keyword)) {
            return inquiryQuestion.inquiryStatus.eq(InquiryStatus.WAITING);
        } else if ("완료".equals(keyword) || "DONE".equalsIgnoreCase(keyword)) {
            return inquiryQuestion.inquiryStatus.eq(InquiryStatus.DONE);
        }
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(InquirySortType sortBy) {
        if (sortBy == InquirySortType.STATUS) {
            return inquiryQuestion.inquiryStatus.asc();
        }
        // 기본값: 작성일시 내림차순
        return inquiryQuestion.createdDate.desc();
    }
}
