package com.quitmate.global.page.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@NoArgsConstructor
public class PageInfoServiceRequest {

    private int page;
    private int size;

    public PageInfoServiceRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public Pageable toPageable() {
        return PageRequest.of(page - 1, size);
    }
}
