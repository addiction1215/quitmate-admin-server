package com.quitmate.cigaretteHistoryBatch.repository;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CigaretteHistoryBatchIssueJpaRepository extends JpaRepository<CigaretteHistoryBatchIssue, Long> {

    Page<CigaretteHistoryBatchIssue> findAllByStatus(CigaretteHistoryBatchIssueStatus status, Pageable pageable);

    /**
     * 지금 상태가 expected일 때만 바꾼다. 앱 서버 배치가 같은 행을 막 닫았으면 0을 돌려준다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CigaretteHistoryBatchIssue i"
            + " set i.status = :next, i.memo = :memo, i.resolvedBy = :resolvedBy,"
            + " i.resolvedAt = :resolvedAt, i.updatedDate = :resolvedAt"
            + " where i.id = :id and i.status = :expected")
    int updateStatusIf(@Param("id") Long id,
                       @Param("expected") CigaretteHistoryBatchIssueStatus expected,
                       @Param("next") CigaretteHistoryBatchIssueStatus next,
                       @Param("memo") String memo,
                       @Param("resolvedBy") Long resolvedBy,
                       @Param("resolvedAt") LocalDateTime resolvedAt);
}
