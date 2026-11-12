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
    private String title;
    private String content;
    /**
     * 공지는 등록했지만 푸시를 보내지 않은 까닭. 관리자 화면에 그대로 띄울 수 있는 문장이다.
     * 푸시를 보내는 절차를 탔으면 null(받을 사람이 없어 0건이어도 null).
     */
    private String pushSkipReason;

    @Builder
    private NoticeCreateResponse(Long id, NoticeType type, String title, String content, String pushSkipReason) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.pushSkipReason = pushSkipReason;
    }

    public static NoticeCreateResponse createResponse(Notice notice) {
        return createResponse(notice, null);
    }

    public static NoticeCreateResponse createResponse(Notice notice, String pushSkipReason) {
        return NoticeCreateResponse.builder()
                .id(notice.getId())
                .type(notice.getType())
                .title(notice.getTitle())
                .content(notice.getContent())
                .pushSkipReason(pushSkipReason)
                .build();
    }
}
