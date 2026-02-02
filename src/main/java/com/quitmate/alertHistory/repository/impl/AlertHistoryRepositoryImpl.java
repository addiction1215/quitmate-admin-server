package com.quitmate.alertHistory.repository.impl;

import com.quitmate.alertHistory.entity.AlertHistory;
import com.quitmate.alertHistory.repository.AlertHistoryJpaRepository;
import com.quitmate.alertHistory.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AlertHistoryRepositoryImpl implements AlertHistoryRepository {

    private final AlertHistoryJpaRepository alertHistoryJpaRepository;

    @Override
    public Optional<AlertHistory> findById(Long id) {
        return alertHistoryJpaRepository.findById(id);
    }

    @Override
    public AlertHistory save(AlertHistory alertHistory) {
        return alertHistoryJpaRepository.save(alertHistory);
    }

    @Override
    public void deleteAllInBatch() {
        alertHistoryJpaRepository.deleteAllInBatch();
    }

    @Override
    public void saveAll(List<AlertHistory> alertHistories) {
        alertHistoryJpaRepository.saveAll(alertHistories);
    }
}
