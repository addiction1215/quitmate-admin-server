package com.quitmate.challenge.missionhistory.service.request;

import com.quitmate.challenge.missionhistory.controller.request.MissionHistorySearchCategory;
import com.quitmate.challenge.missionhistory.controller.request.MissionHistorySortType;
import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryStatusFilter;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MissionHistoryListServiceRequest extends PageInfoServiceRequest {

    private final MissionHistorySortType sortBy;
    private final MissionHistorySearchCategory category;
    private final String keyword;
    private final MissionHistoryStatusFilter status;

    @Builder
    public MissionHistoryListServiceRequest(int page, int size, MissionHistorySearchCategory category,
                                            MissionHistorySortType sortBy, String keyword,
                                            MissionHistoryStatusFilter status) {
        super(page, size);
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
        this.status = status;
    }
}
