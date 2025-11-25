package com.quitmate.notice.controller.request;

import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeCreateRequest {

    @NotNull(message = "유형은 필수입니다.")
    private NoticeType type;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Builder
    public NoticeCreateRequest(NoticeType type, String content) {
        this.type = type;
        this.content = content;
    }

    public NoticeCreateServiceRequest toServiceRequest() {
        return NoticeCreateServiceRequest.builder()
                .type(type)
                .content(content)
                .build();
    }
}
