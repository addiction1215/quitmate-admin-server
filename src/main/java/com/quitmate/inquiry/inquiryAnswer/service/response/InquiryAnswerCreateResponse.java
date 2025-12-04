package com.quitmate.inquiry.inquiryAnswer.service.response;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryAnswerCreateResponse {

    private Long answerId;
    private Long inquiryQuestionId;
    private String content;

    @Builder
    private InquiryAnswerCreateResponse(Long answerId, Long inquiryQuestionId, String content) {
        this.answerId = answerId;
        this.inquiryQuestionId = inquiryQuestionId;
        this.content = content;
    }

    public static InquiryAnswerCreateResponse createResponse(InquiryAnswer inquiryAnswer) {
        return InquiryAnswerCreateResponse.builder()
                .answerId(inquiryAnswer.getId())
                .inquiryQuestionId(inquiryAnswer.getInquiryQuestion().getId())
                .content(inquiryAnswer.getContent())
                .build();
    }
}
