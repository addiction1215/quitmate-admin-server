package com.quitmate.notice.controller.request;

import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeUpdateRequest {

    @NotNull(message = "공지사항 ID는 필수입니다.")
    private Long id;

    @NotNull(message = "유형은 필수입니다.")
    private NoticeType type;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하입니다.")
    private String title;

    /**
     * 글자 본문. contentHtml 을 함께 보내면 서버가 그 글자 버전으로 덮어쓴다(그때는 비워도 된다).
     */
    private String content;

    /**
     * 서식 본문(선택). 보내면 서버가 허용 태그만 남기고 정리해 저장한다.
     */
    private String contentHtml;

    @Builder
    public NoticeUpdateRequest(Long id, NoticeType type, String title, String content, String contentHtml) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.contentHtml = contentHtml;
    }

    public NoticeUpdateServiceRequest toServiceRequest() {
        return NoticeUpdateServiceRequest.builder()
                .id(id)
                .type(type)
                .title(title)
                .content(content)
                .contentHtml(contentHtml)
                .build();
    }
}
