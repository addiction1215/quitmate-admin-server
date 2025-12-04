package com.quitmate.inquiry.inquryQuestion.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.inquiry.inquryQuestion.enums.InquirySearchCategory;
import com.quitmate.inquiry.inquryQuestion.enums.InquirySortType;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
import lombok.Getter;

@Getter
public class InquiryQuestionListRequest extends PageInfoRequest {

    private final InquirySortType sortBy;
    private final InquirySearchCategory category;
    private final String keyword;

    public InquiryQuestionListRequest(InquirySearchCategory category, String keyword, InquirySortType sortBy) {
        this.category = category;
        this.keyword = keyword;
        this.sortBy = sortBy;
    }

    public InquiryQuestionListServiceRequest toServiceRequest() {
        return InquiryQuestionListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
