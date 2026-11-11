package com.quitmate.cigaretteHistoryBatch.repository;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CigaretteHistoryBatchIssueRepository {

    Page<CigaretteHistoryBatchIssue> findAllByStatus(CigaretteHistoryBatchIssueStatus status, Pageable pageable);

    Optional<CigaretteHistoryBatchIssue> findById(Long id);

    /**
     * PENDING인 행만 IGNORED로 바꾼다.
     *
     * @return 바뀐 행 수. 0이면 없는 행이거나 이미 다른 상태다.
     */
    int ignoreIfPending(Long id, String memo, Long adminId, LocalDateTime ignoredAt);
}
