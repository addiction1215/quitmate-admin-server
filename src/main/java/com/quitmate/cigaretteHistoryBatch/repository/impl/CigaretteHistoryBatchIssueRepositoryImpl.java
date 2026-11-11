package com.quitmate.cigaretteHistoryBatch.repository.impl;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.cigaretteHistoryBatch.repository.CigaretteHistoryBatchIssueJpaRepository;
import com.quitmate.cigaretteHistoryBatch.repository.CigaretteHistoryBatchIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CigaretteHistoryBatchIssueRepositoryImpl implements CigaretteHistoryBatchIssueRepository {

    private final CigaretteHistoryBatchIssueJpaRepository cigaretteHistoryBatchIssueJpaRepository;

    @Override
    public Page<CigaretteHistoryBatchIssue> findAllByStatus(CigaretteHistoryBatchIssueStatus status, Pageable pageable) {
        return cigaretteHistoryBatchIssueJpaRepository.findAllByStatus(status, pageable);
    }

    @Override
    public Optional<CigaretteHistoryBatchIssue> findById(Long id) {
        return cigaretteHistoryBatchIssueJpaRepository.findById(id);
    }

    @Override
    public int ignoreIfPending(Long id, String memo, Long adminId, LocalDateTime ignoredAt) {
        return cigaretteHistoryBatchIssueJpaRepository.updateStatusIf(id, CigaretteHistoryBatchIssueStatus.PENDING,
                CigaretteHistoryBatchIssueStatus.IGNORED, memo, adminId, ignoredAt);
    }
}
