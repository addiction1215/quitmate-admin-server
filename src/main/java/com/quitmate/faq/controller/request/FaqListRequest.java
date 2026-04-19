package com.quitmate.faq.controller.request;

import com.quitmate.faq.service.request.FaqListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FaqListRequest extends PageInfoRequest {

    private final String keyword;

    @Builder
    public FaqListRequest(int page, int size, String keyword) {
        super(page, size);
        this.keyword = keyword;
    }

    public FaqListServiceRequest toServiceRequest() {
        return FaqListServiceRequest.builder()
                .page(getPage())
                .size(getSize())
                .keyword(keyword)
                .build();
    }
}
