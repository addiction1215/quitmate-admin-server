package com.quitmate.challenge.challange.service.request;

import com.quitmate.challenge.challange.controller.request.SearchCategory;
import com.quitmate.challenge.challange.controller.request.SortType;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ChallengeSearchServiceRequest extends PageInfoServiceRequest {

    private final SearchCategory category;
    private final String keyword;
    private final SortType sortType;

    @Builder
    public ChallengeSearchServiceRequest(SearchCategory category, String keyword, SortType sortType, int page, int size) {
        super(page, size);
        this.category = category;
        this.keyword = keyword;
        this.sortType = sortType;
    }

}
