package com.quitmate.challenge.rewardHistory.controller.request;

import com.quitmate.challenge.rewardHistory.enums.RewardSearchCategory;
import com.quitmate.challenge.rewardHistory.enums.RewardSortType;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RewardHistoryListRequest extends PageInfoRequest {

    private RewardSortType sortBy;          // 정렬 기준: CREATED_DATE 또는 USER_NAME (선택)
    private RewardSearchCategory category;  // 검색 카테고리: CREATED_DATE, USER_NAME, TYPE (선택)
    private String keyword;                 // 검색어 (선택)

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
