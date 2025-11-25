package com.quitmate.challenge.rewardHistory.service.request;

import com.quitmate.challenge.rewardHistory.enums.RewardSearchCategory;
import com.quitmate.challenge.rewardHistory.enums.RewardSortType;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class RewardHistoryListServiceRequest extends PageInfoServiceRequest {

    private final RewardSortType sortBy;
    private final RewardSearchCategory category;
    private final String keyword;

    @Builder
    public RewardHistoryListServiceRequest(int page, int size, RewardSearchCategory category, RewardSortType sortBy, String keyword) {
        super(page, size);
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
    }
}
