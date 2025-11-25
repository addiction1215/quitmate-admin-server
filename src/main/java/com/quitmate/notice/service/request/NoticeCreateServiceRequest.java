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
    private String content;

    @Builder
    public NoticeCreateServiceRequest(NoticeType type, String content) {
        this.type = type;
        this.content = content;
    }

    public Notice toEntity() {
        return Notice.builder()
                .type(type)
                .content(content)
                .build();
    }
}
