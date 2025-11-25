package com.quitmate.challenge.rewardHistory.service.impl;

import com.quitmate.challenge.rewardHistory.repository.RewardHistoryRepository;
import com.quitmate.challenge.rewardHistory.repository.response.RewardHistoryDto;
import com.quitmate.challenge.rewardHistory.service.RewardHistoryReadService;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import com.quitmate.challenge.rewardHistory.service.response.RewardHistoryListResponse;
import com.quitmate.global.page.response.PageCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardHistoryReadServiceImpl implements RewardHistoryReadService {

    private final RewardHistoryRepository rewardHistoryRepository;

    @Override
    public PageCustom<RewardHistoryListResponse> getRewardHistoryList(RewardHistoryListServiceRequest request) {
        Pageable pageable = request.toPageable();
        Page<RewardHistoryDto> rewardHistoryDtoPage = rewardHistoryRepository.findRewardHistoryList(request, pageable);
        
        Page<RewardHistoryListResponse> responsePage = rewardHistoryDtoPage.map(RewardHistoryListResponse::of);
        
        return PageCustom.of(responsePage);
    }
}
