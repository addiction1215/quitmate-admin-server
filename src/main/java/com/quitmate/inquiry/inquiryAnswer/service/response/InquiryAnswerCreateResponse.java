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
    private LocalDateTime createdDate;

    @Builder
    private InquiryAnswerCreateResponse(Long answerId, Long inquiryQuestionId, String content, LocalDateTime createdDate) {
        this.answerId = answerId;
        this.inquiryQuestionId = inquiryQuestionId;
        this.content = content;
        this.createdDate = createdDate;
    }

    public static InquiryAnswerCreateResponse createResponse(InquiryAnswer inquiryAnswer) {
        return InquiryAnswerCreateResponse.builder()
                .answerId(inquiryAnswer.getId())
                .inquiryQuestionId(inquiryAnswer.getInquiryQuestion().getId())
                .content(inquiryAnswer.getContent())
                .createdDate(inquiryAnswer.getCreatedDate())
                .build();
    }
}
