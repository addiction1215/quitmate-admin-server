package com.quitmate.challenge.rewardHistory.repository;

import com.quitmate.challenge.rewardHistory.entity.RewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardHistoryJpaRepository extends JpaRepository<RewardHistory, Long> {
}
