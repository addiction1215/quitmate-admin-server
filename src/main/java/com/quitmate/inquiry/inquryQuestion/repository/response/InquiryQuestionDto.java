package com.quitmate.inquiry.inquryQuestion.repository.response;

import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InquiryQuestionDto {
    private Long id;                        // 문의 ID
    private String writerName;              // 작성자
    private String email;                   // 작성자 이메일
    private String title;                   // 문의 제목
    private LocalDateTime createdDate;      // 작성일시
    private InquiryStatus status;           // 상태

    public InquiryQuestionDto(Long id, String writerName, String email, String title,
                              LocalDateTime createdDate, InquiryStatus status) {
        this.id = id;
        this.writerName = writerName;
        this.email = email;
        this.title = title;
        this.createdDate = createdDate;
        this.status = status;
    }
}
