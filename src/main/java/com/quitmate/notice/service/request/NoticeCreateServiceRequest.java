package com.quitmate.notice.service.request;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeCreateServiceRequest {

    private NoticeType type;
    private String title;
    private String content;

    @Builder
    public NoticeCreateServiceRequest(NoticeType type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public Notice toEntity() {
        return Notice.builder()
                .type(type)
                .title(title)
                .content(content)
                .build();
    }
}
