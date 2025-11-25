package com.quitmate.challenge.rewardHistory.repository;

import com.quitmate.challenge.rewardHistory.entity.RewardHistory;
import com.quitmate.challenge.rewardHistory.repository.response.RewardHistoryDto;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RewardHistoryRepository {

    List<RewardHistory> findAll();

    RewardHistory save(RewardHistory rewardHistory);

    Optional<RewardHistory> findById(Long id);

    void deleteAllInBatch();

    void saveAll(List<RewardHistory> rewardHistories);

    Page<RewardHistoryDto> findRewardHistoryList(RewardHistoryListServiceRequest request, Pageable pageable);
}
