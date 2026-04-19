package com.quitmate.faq.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateServiceRequest {

    private Long id;
    private String title;
    private String description;

    @Builder
    public FaqUpdateServiceRequest(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }
}
