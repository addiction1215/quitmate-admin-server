package com.quitmate.faq.service.response;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.enums.FaqCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateResponse {

    private Long id;
    private FaqCategory category;
    private Boolean pinned;
    private Integer sortOrder;
    private String title;
    private String description;

    @Builder
    public FaqUpdateResponse(Long id, FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.id = id;
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public static FaqUpdateResponse createResponse(Faq faq) {
        return FaqUpdateResponse.builder()
                .id(faq.getId())
                .category(faq.getCategory())
                .pinned(faq.getPinned())
                .sortOrder(faq.getSortOrder())
                .title(faq.getTitle())
                .description(faq.getDescription())
                .build();
    }
}
