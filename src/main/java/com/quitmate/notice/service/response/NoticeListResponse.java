package com.quitmate.notice.service.response;

import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.response.NoticeDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeListResponse {

    private Long id;                        // 공지사항 ID
    private String type;                // 유형
    private String content;                 // 내용
    private LocalDateTime createdDate;      // 작성일시

    @Builder
    private NoticeListResponse(Long id, String type, String content, LocalDateTime createdDate) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.createdDate = createdDate;
    }

    public static NoticeListResponse createResponse(NoticeDto dto) {
        return NoticeListResponse.builder()
                .id(dto.getId())
                .type(dto.getType().getDescription())
                .content(dto.getContent())
                .createdDate(dto.getCreatedDate())
                .build();
    }
}
