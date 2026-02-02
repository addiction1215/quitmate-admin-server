package com.quitmate.alertHistory.repository;

import com.quitmate.alertHistory.entity.AlertHistory;

import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository {

    Optional<AlertHistory> findById(Long id);

    AlertHistory save(AlertHistory alertHistory);

    void deleteAllInBatch();

    void saveAll(List<AlertHistory> alertHistories);
}
