package com.quitmate.faq.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FaqListServiceRequest extends PageInfoServiceRequest {

    private final String keyword;

    @Builder
    public FaqListServiceRequest(int page, int size, String keyword) {
        super(page, size);
        this.keyword = keyword;
    }
}
