package com.quitmate.faq.service.request;

import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FaqListServiceRequest extends PageInfoServiceRequest {

    private final FaqCategory category;
    private final String keyword;

    @Builder
    public FaqListServiceRequest(int page, int size, FaqCategory category, String keyword) {
        super(page, size);
        this.category = category;
        this.keyword = keyword;
    }
}
