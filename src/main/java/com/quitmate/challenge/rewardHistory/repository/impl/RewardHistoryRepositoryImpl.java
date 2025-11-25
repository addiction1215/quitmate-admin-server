package com.quitmate.challenge.rewardHistory.repository.impl;

import com.quitmate.challenge.rewardHistory.entity.RewardHistory;
import com.quitmate.challenge.rewardHistory.repository.RewardHistoryJpaRepository;
import com.quitmate.challenge.rewardHistory.repository.RewardHistoryQueryRepository;
import com.quitmate.challenge.rewardHistory.repository.RewardHistoryRepository;
import com.quitmate.challenge.rewardHistory.repository.response.RewardHistoryDto;
import com.quitmate.challenge.rewardHistory.service.request.RewardHistoryListServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardHistoryRepositoryImpl implements RewardHistoryRepository {

    private final RewardHistoryJpaRepository rewardHistoryJpaRepository;
    private final RewardHistoryQueryRepository rewardHistoryQueryRepository;

    @Override
    public List<RewardHistory> findAll() {
        return rewardHistoryJpaRepository.findAll();
    }

    @Override
    public RewardHistory save(RewardHistory rewardHistory) {
        return rewardHistoryJpaRepository.save(rewardHistory);
    }

    @Override
    public Optional<RewardHistory> findById(Long id) {
        return rewardHistoryJpaRepository.findById(id);
    }

    @Override
    public void deleteAllInBatch() {
        rewardHistoryJpaRepository.deleteAllInBatch();
    }

    @Override
    public void saveAll(List<RewardHistory> rewardHistories) {
        rewardHistoryJpaRepository.saveAll(rewardHistories);
    }

    @Override
    public Page<RewardHistoryDto> findRewardHistoryList(RewardHistoryListServiceRequest request, Pageable pageable) {
        return rewardHistoryQueryRepository.findRewardHistoryList(request, pageable);
    }
}
