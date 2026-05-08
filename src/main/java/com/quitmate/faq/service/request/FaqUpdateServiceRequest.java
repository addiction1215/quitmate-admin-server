package com.quitmate.faq.service.request;

import com.quitmate.faq.enums.FaqCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateServiceRequest {

    private Long id;
    private FaqCategory category;
    private Boolean pinned;
    private Integer sortOrder;
    private String title;
    private String description;

    @Builder
    public FaqUpdateServiceRequest(Long id, FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.id = id;
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }
}
