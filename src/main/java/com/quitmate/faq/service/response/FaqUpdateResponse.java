package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateResponse {

    private Long id;
    private String title;
    private String description;

    @Builder
    public FaqUpdateResponse(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public static FaqUpdateResponse createResponse(Faq faq) {
        return FaqUpdateResponse.builder()
                .id(faq.getId())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
