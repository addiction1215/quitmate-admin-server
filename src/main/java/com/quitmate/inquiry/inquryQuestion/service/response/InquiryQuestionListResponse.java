package com.quitmate.inquiry.inquryQuestion.service.response;

import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.inquiry.inquryQuestion.repository.response.InquiryQuestionDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryQuestionListResponse {

    private Long id;
    private String writerName;
    private String title;
    private LocalDateTime createdDate;
    private InquiryStatus status;

    @Builder
    private InquiryQuestionListResponse(Long id, String writerName, String title, 
                                       LocalDateTime createdDate, InquiryStatus status) {
        this.id = id;
        this.writerName = writerName;
        this.title = title;
        this.createdDate = createdDate;
        this.status = status;
    }

    public static InquiryQuestionListResponse createResponse(InquiryQuestionDto dto) {
        return InquiryQuestionListResponse.builder()
                .id(dto.getId())
                .writerName(dto.getWriterName())
                .title(dto.getTitle())
                .createdDate(dto.getCreatedDate())
                .status(dto.getStatus())
                .build();
    }
}
