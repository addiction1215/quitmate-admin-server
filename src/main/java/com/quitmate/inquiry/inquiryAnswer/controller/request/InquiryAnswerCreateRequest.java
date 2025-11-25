package com.quitmate.inquiry.inquiryAnswer.controller.request;

import com.quitmate.inquiry.inquiryAnswer.service.request.InquiryAnswerCreateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryAnswerCreateRequest {

    @NotNull(message = "문의사항 ID는 필수입니다.")
    private Long inquiryQuestionId;

    @NotBlank(message = "답변 내용은 필수입니다.")
    private String content;

    @Builder
    public InquiryAnswerCreateRequest(Long inquiryQuestionId, String content) {
        this.inquiryQuestionId = inquiryQuestionId;
        this.content = content;
    }

    public InquiryAnswerCreateServiceRequest toServiceRequest() {
        return InquiryAnswerCreateServiceRequest.builder()
                .inquiryQuestionId(inquiryQuestionId)
                .content(content)
                .build();
    }
}
