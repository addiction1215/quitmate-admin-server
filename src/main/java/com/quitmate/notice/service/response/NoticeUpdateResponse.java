package com.quitmate.notice.service.response;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeUpdateResponse {

    private Long id;
    private NoticeType type;
    private String content;
    private LocalDateTime updatedDate;

    @Builder
    private NoticeUpdateResponse(Long id, NoticeType type, String content, LocalDateTime updatedDate) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.updatedDate = updatedDate;
    }

    public static NoticeUpdateResponse of(Notice notice) {
        return NoticeUpdateResponse.builder()
                .id(notice.getId())
                .type(notice.getType())
                .content(notice.getContent())
                .updatedDate(notice.getModifiedDate())
                .build();
    }
}
