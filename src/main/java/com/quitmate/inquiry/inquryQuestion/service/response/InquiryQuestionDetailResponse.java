package com.quitmate.inquiry.inquryQuestion.service.response;

import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryQuestionDetailResponse {

    private Long id;
    private String writerName;
    private String title;
    private String question;
    private LocalDateTime createdDate;
    private InquiryStatus status;
    private InquiryAnswerInfo answer;

    @Builder
    private InquiryQuestionDetailResponse(Long id, String writerName, String title, String question,
                                         LocalDateTime createdDate, InquiryStatus status, InquiryAnswerInfo answer) {
        this.id = id;
        this.writerName = writerName;
        this.title = title;
        this.question = question;
        this.createdDate = createdDate;
        this.status = status;
        this.answer = answer;
    }

    public static InquiryQuestionDetailResponse createResponse(InquiryQuestion inquiryQuestion, InquiryAnswer inquiryAnswer) {
        return InquiryQuestionDetailResponse.builder()
                .id(inquiryQuestion.getId())
                .writerName(inquiryQuestion.getUser().getNickName())
                .title(inquiryQuestion.getTitle())
                .question(inquiryQuestion.getQuestion())
                .createdDate(inquiryQuestion.getCreatedDate())
                .status(inquiryQuestion.getInquiryStatus())
                .answer(inquiryAnswer != null ? InquiryAnswerInfo.of(inquiryAnswer) : null)
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class InquiryAnswerInfo {
        private Long answerId;
        private String content;
        private LocalDateTime createdDate;

        @Builder
        private InquiryAnswerInfo(Long answerId, String content, LocalDateTime createdDate) {
            this.answerId = answerId;
            this.content = content;
            this.createdDate = createdDate;
        }

        public static InquiryAnswerInfo of(InquiryAnswer inquiryAnswer) {
            return InquiryAnswerInfo.builder()
                    .answerId(inquiryAnswer.getId())
                    .content(inquiryAnswer.getContent())
                    .createdDate(inquiryAnswer.getCreatedDate())
                    .build();
        }
    }
}
