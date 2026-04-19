package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateResponse {

    private Long id;
    private String title;
    private String description;

    @Builder
    public FaqCreateResponse(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public static FaqCreateResponse createResponse(Faq faq) {
        return FaqCreateResponse.builder()
                .id(faq.getId())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
