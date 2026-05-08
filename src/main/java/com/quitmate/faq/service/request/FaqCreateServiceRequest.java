package com.quitmate.faq.service.request;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.enums.FaqCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqCreateServiceRequest {

    private FaqCategory category;
    private Boolean pinned;
    private Integer sortOrder;
    private String title;
    private String description;

    @Builder
    public FaqCreateServiceRequest(FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public Faq toEntity() {
        return Faq.builder()
                .category(category)
                .pinned(pinned)
                .sortOrder(sortOrder)
                .title(title)
                .description(description)
                .build();
    }
}
