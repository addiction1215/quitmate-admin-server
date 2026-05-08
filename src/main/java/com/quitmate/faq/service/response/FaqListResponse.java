package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.enums.FaqCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FaqListResponse {

    private Long id;
    private FaqCategory category;
    private Boolean pinned;
    private Integer sortOrder;
    private String title;
    private String description;

    public static FaqListResponse of(Faq faq) {
        return FaqListResponse.builder()
                .id(faq.getId())
                .category(faq.getCategory())
                .pinned(faq.getPinned())
                .sortOrder(faq.getSortOrder())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
