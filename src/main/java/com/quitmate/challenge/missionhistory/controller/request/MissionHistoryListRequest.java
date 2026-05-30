package com.quitmate.challenge.missionhistory.controller.request;

import com.quitmate.challenge.missionhistory.service.request.MissionHistoryListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Getter;

@Getter
public class MissionHistoryListRequest extends PageInfoRequest {

    private final MissionHistorySortType sortBy;
    private final MissionHistorySearchCategory category;
    private final String keyword;
    private final MissionHistoryStatusFilter status;

    public MissionHistoryListRequest(MissionHistorySearchCategory category, String keyword, MissionHistorySortType sortBy,
                                     MissionHistoryStatusFilter status) {
        this.category = category;
        this.keyword = keyword;
        this.sortBy = sortBy;
        this.status = status;
    }

    public MissionHistoryListServiceRequest toServiceRequest() {
        return MissionHistoryListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .status(status)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
