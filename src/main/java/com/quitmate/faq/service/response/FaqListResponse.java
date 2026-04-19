package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FaqListResponse {

    private Long id;
    private String title;
    private String description;

    public static FaqListResponse of(Faq faq) {
        return FaqListResponse.builder()
                .id(faq.getId())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
