package com.quitmate.notice.service.request;

import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateServiceRequest {

    private Long id;
    private NoticeType type;
    private String content;

    @Builder
    public NoticeUpdateServiceRequest(Long id, NoticeType type, String content) {
        this.id = id;
        this.type = type;
        this.content = content;
    }
}
