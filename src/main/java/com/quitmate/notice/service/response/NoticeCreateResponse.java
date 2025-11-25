package com.quitmate.notice.service.response;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeCreateResponse {

    private Long id;
    private NoticeType type;
    private String content;
    private LocalDateTime createdDate;

    @Builder
    private NoticeCreateResponse(Long id, NoticeType type, String content, LocalDateTime createdDate) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.createdDate = createdDate;
    }

    public static NoticeCreateResponse of(Notice notice) {
        return NoticeCreateResponse.builder()
                .id(notice.getId())
                .type(notice.getType())
                .content(notice.getContent())
                .createdDate(notice.getCreatedDate())
                .build();
    }
}
