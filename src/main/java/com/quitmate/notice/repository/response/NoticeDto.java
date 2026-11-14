package com.quitmate.notice.repository.response;

import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.enums.NoticeType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeDto {
    private Long id;                        // 공지사항 ID
    private NoticeType type;                // 유형
    private String title;                   // 제목
    private String content;                 // 내용
    private LocalDateTime createdDate;      // 작성일시
    private NoticePushStatus pushStatus;    // 푸시 발송 상태
    private LocalDateTime pushScheduledAt;  // 보류한 푸시를 보낼 시각(한국 시각)
    private LocalDateTime pushSentAt;       // 푸시를 보내기 시작한 시각(한국 시각)

    public NoticeDto(Long id, NoticeType type, String title, String content, LocalDateTime createdDate,
                     NoticePushStatus pushStatus, LocalDateTime pushScheduledAt, LocalDateTime pushSentAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate;
        this.pushStatus = pushStatus;
        this.pushScheduledAt = pushScheduledAt;
        this.pushSentAt = pushSentAt;
    }
}
