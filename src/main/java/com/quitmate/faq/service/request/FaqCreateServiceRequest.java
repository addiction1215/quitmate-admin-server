package com.quitmate.faq.service.request;

import com.quitmate.faq.entity.Faq;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateServiceRequest {

    private String title;
    private String description;

    @Builder
    public FaqCreateServiceRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Faq toEntity() {
        return Faq.builder()
                .title(title)
                .description(description)
                .build();
    }
}
