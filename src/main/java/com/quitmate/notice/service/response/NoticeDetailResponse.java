package com.quitmate.notice.service.response;

import com.quitmate.notice.entity.Notice;
import com.quitmate.notice.enums.NoticeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticeDetailResponse {

    private Long id;
    private NoticeType type;
    private String title;
    private String content;
    /** 서식 본문. 이미지 src 는 1시간 서명 URL(편집·미리보기용), data-key 는 그대로 있다. 서식 없는 공지는 null. */
    private String contentHtml;
    private LocalDateTime createdDate;

    @Builder
    private NoticeDetailResponse(Long id, NoticeType type, String title, String content, String contentHtml,
                                 LocalDateTime createdDate) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.contentHtml = contentHtml;
        this.createdDate = createdDate;
    }

    public static NoticeDetailResponse createResponse(Notice notice, String contentHtmlWithSrc) {
        return NoticeDetailResponse.builder()
                .id(notice.getId())
                .type(notice.getType())
                .title(notice.getTitle())
                .content(notice.getContent())
                .contentHtml(contentHtmlWithSrc)
                .createdDate(notice.getCreatedDate())
                .build();
    }
}
