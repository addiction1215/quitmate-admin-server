package com.quitmate.inquiry.inquiryAnswer.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryAnswerCreateServiceRequest {

    private Long inquiryQuestionId;
    private String content;

    @Builder
    public InquiryAnswerCreateServiceRequest(Long inquiryQuestionId, String content) {
        this.inquiryQuestionId = inquiryQuestionId;
        this.content = content;
    }
}
