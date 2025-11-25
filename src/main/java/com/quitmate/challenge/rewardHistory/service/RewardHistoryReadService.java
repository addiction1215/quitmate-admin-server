package com.quitmate.challenge.rewardHistory.service;

import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import com.quitmate.challenge.rewardHistory.service.response.RewardHistoryListResponse;
import com.quitmate.global.page.response.PageCustom;

public interface RewardHistoryReadService {

    PageCustom<RewardHistoryListResponse> getRewardHistoryList(RewardHistoryListServiceRequest request);
}
