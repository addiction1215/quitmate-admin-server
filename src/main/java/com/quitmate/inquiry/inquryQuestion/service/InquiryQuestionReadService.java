package com.quitmate.inquiry.inquryQuestion.service;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.service.request.InquiryQuestionListServiceRequest;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionDetailResponse;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionListResponse;

public interface InquiryQuestionReadService {
    InquiryQuestion findById(Long id);

    PageCustom<InquiryQuestionListResponse> getInquiryQuestionList(InquiryQuestionListServiceRequest request);

    InquiryQuestionDetailResponse getInquiryQuestionDetail(Long id);
}
