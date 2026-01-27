package com.quitmate.challenge.missionhistory.controller.request;

import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Getter;

@Getter
public class MissionHistoryListRequest extends PageInfoRequest {

    private final MissionHistorySortType sortBy;
    private final MissionHistorySearchCategory category;
    private final String keyword;

    public MissionHistoryListRequest(MissionHistorySearchCategory category, String keyword, MissionHistorySortType sortBy) {
        this.category = category;
        this.keyword = keyword;
        this.sortBy = sortBy;
    }

    public MissionHistoryListServiceRequest toServiceRequest() {
        return MissionHistoryListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
