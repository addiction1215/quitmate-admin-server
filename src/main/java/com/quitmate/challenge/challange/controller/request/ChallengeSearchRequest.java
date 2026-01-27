package com.quitmate.challenge.challange.controller.request;

import com.quitmate.challenge.challange.service.request.ChallengeSearchServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChallengeSearchRequest extends PageInfoRequest {

    private SearchCategory category;
    private String keyword;
    private SortType sortType;

    public ChallengeSearchRequest(SearchCategory category, String keyword, SortType sortType) {
        this.category = category;
        this.keyword = keyword;
        this.sortType = sortType;
    }

    public ChallengeSearchServiceRequest toServiceRequest() {
        return ChallengeSearchServiceRequest.builder()
                .category(this.category)
                .keyword(this.keyword)
                .sortType(this.sortType)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
