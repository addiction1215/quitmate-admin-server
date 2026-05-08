package com.quitmate.faq.controller.request;

import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.service.request.FaqListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FaqListRequest extends PageInfoRequest {

    private final FaqCategory category;
    private final String keyword;

    @Builder
    public FaqListRequest(int page, int size, FaqCategory category, String keyword) {
        super(page, size);
        this.category = category;
        this.keyword = keyword;
    }

    public FaqListServiceRequest toServiceRequest() {
        return FaqListServiceRequest.builder()
                .page(getPage())
                .size(getSize())
                .category(category)
                .keyword(keyword)
                .build();
    }
}
