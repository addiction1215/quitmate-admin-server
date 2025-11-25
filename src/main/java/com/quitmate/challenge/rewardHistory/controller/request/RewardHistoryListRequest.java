package com.quitmate.challenge.rewardHistory.controller.request;

import com.quitmate.challenge.rewardHistory.enums.RewardSearchCategory;
import com.quitmate.challenge.rewardHistory.enums.RewardSortType;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class RewardHistoryListRequest extends PageInfoRequest {

    private final RewardSortType sortBy;          // 정렬 기준: CREATED_DATE 또는 USER_NAME (선택)
    private final RewardSearchCategory category;  // 검색 카테고리: CREATED_DATE, USER_NAME, TYPE (선택)
    private final String keyword;                 // 검색어 (선택)

    public RewardHistoryListRequest(int page, int size, RewardSearchCategory category, RewardSortType sortBy, String keyword) {
        super(page, size);
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
    }

    public RewardHistoryListServiceRequest toServiceRequest() {
        return RewardHistoryListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
