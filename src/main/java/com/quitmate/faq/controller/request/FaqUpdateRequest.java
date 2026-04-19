package com.quitmate.faq.controller.request;

import com.quitmate.faq.service.request.FaqUpdateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateRequest {

    @NotNull(message = "FAQ ID는 필수입니다.")
    private Long id;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String description;

    @Builder
    public FaqUpdateRequest(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public FaqUpdateServiceRequest toServiceRequest() {
        return FaqUpdateServiceRequest.builder()
                .id(id)
                .title(title)
                .description(description)
                .build();
    }
}
