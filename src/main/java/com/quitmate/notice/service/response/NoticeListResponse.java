package com.quitmate.notice.service.response;

import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.response.NoticeDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeListResponse {

    private Long id;                        // 공지사항 ID
    private NoticeType type;                // 유형
    private String title;                   // 제목
    private String content;                 // 내용
    private LocalDateTime createdDate;      // 작성일시
    private NoticePushStatus pushStatus;    // 푸시 발송 상태(이 칸이 생기기 전 공지는 null)
    private LocalDateTime pushScheduledAt;  // 보류한 푸시를 보낼 시각(한국 시각)
    private LocalDateTime pushSentAt;       // 푸시를 보내기 시작한 시각(한국 시각)

    @Builder
    private NoticeListResponse(Long id, NoticeType type, String title, String content, LocalDateTime createdDate,
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

    public static NoticeListResponse createResponse(NoticeDto dto) {
        return NoticeListResponse.builder()
                .id(dto.getId())
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .createdDate(dto.getCreatedDate())
                .pushStatus(dto.getPushStatus())
                .pushScheduledAt(dto.getPushScheduledAt())
                .pushSentAt(dto.getPushSentAt())
                .build();
    }
}
