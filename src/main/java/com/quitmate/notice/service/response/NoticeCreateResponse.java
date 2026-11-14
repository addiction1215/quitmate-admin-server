package com.quitmate.notice.service.response;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticePushStatus;
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
     * 공지는 등록했지만 푸시를 바로 보내지 않은 까닭. 관리자 화면에 그대로 띄울 수 있는 문장이다.
     * 지금은 야간에 등록한 이벤트 공지(푸시 보류)에만 채운다. 바로 보냈으면 null(받을 사람이 없어 0건이어도 null).
     */
    private String pushSkipReason;
    /** 푸시 발송 상태. 바로 보냈으면 SENT, 보류했으면 DEFERRED */
    private NoticePushStatus pushStatus;
    /** 보류한 푸시를 보낼 시각(한국 시각). 보류하지 않았으면 null */
    private LocalDateTime pushScheduledAt;

    @Builder
    private NoticeCreateResponse(Long id, NoticeType type, String title, String content, String pushSkipReason,
                                 NoticePushStatus pushStatus, LocalDateTime pushScheduledAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.pushSkipReason = pushSkipReason;
        this.pushStatus = pushStatus;
        this.pushScheduledAt = pushScheduledAt;
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
                .pushStatus(notice.getPushStatus())
                .pushScheduledAt(notice.getPushScheduledAt())
                .build();
    }
}
