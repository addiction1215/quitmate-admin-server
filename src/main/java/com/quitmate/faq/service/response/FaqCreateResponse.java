package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.enums.FaqCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateResponse {

    private Long id;
    private FaqCategory category;
    private Boolean pinned;
    private Integer sortOrder;
    private String title;
    private String description;

    @Builder
    public FaqCreateResponse(Long id, FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.id = id;
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public static FaqCreateResponse createResponse(Faq faq) {
        return FaqCreateResponse.builder()
                .id(faq.getId())
                .category(faq.getCategory())
                .pinned(faq.getPinned())
                .sortOrder(faq.getSortOrder())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
