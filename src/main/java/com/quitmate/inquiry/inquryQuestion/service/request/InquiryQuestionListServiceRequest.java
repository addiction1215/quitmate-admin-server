package com.quitmate.inquiry.inquryQuestion.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.inquiry.inquryQuestion.enums.InquirySearchCategory;
import com.quitmate.inquiry.inquryQuestion.enums.InquirySortType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class InquiryQuestionListServiceRequest extends PageInfoServiceRequest {

    private final InquirySortType sortBy;
    private final InquirySearchCategory category;
    private final String keyword;

    @Builder
    public InquiryQuestionListServiceRequest(int page, int size, InquirySearchCategory category, InquirySortType sortBy, String keyword) {
        super(page, size);
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
    }
}
