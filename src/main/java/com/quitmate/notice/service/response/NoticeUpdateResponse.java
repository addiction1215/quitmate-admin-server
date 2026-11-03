package com.quitmate.notice.service.response;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateResponse {

    private Long id;
    private NoticeType type;
    private String title;
    private String content;

    @Builder
    private NoticeUpdateResponse(Long id, NoticeType type, String title, String content) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public static NoticeUpdateResponse createResponse(Notice notice) {
        return NoticeUpdateResponse.builder()
                .id(notice.getId())
                .type(notice.getType())
                .title(notice.getTitle())
                .content(notice.getContent())
                .build();
    }
}
