package com.quitmate.faq.controller.request;

import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String description;

    @Builder
    public FaqCreateRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public FaqCreateServiceRequest toServiceRequest() {
        return FaqCreateServiceRequest.builder()
                .title(title)
                .description(description)
                .build();
    }
}
