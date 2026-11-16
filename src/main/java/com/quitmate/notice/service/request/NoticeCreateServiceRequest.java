package com.quitmate.notice.service.request;

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
    private String contentHtml;

    @Builder
    public NoticeCreateServiceRequest(NoticeType type, String title, String content, String contentHtml) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.contentHtml = contentHtml;
    }
}
