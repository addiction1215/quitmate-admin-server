package com.quitmate.notice.repository.response;

import com.quitmate.notice.enums.NoticeType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeDto {
    private Long id;                        // 공지사항 ID
    private NoticeType type;                // 유형
    private String content;                 // 내용
    private LocalDateTime createdDate;      // 작성일시

    public NoticeDto(Long id, NoticeType type, String content, LocalDateTime createdDate) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.createdDate = createdDate;
    }
}
