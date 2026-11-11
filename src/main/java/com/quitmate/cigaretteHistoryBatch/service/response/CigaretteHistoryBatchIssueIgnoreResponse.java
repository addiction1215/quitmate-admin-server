package com.quitmate.cigaretteHistoryBatch.service.response;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CigaretteHistoryBatchIssueIgnoreResponse {

    private final Long id;
    private final String targetDate;
    private final Long userId;
    private final CigaretteHistoryBatchIssueStatus status;
    private final String memo;
    private final Long resolvedBy;
    private final LocalDateTime resolvedAt;

    @Builder
    public CigaretteHistoryBatchIssueIgnoreResponse(Long id, String targetDate, Long userId,
                                                    CigaretteHistoryBatchIssueStatus status, String memo,
                                                    Long resolvedBy, LocalDateTime resolvedAt) {
        this.id = id;
        this.targetDate = targetDate;
        this.userId = userId;
        this.status = status;
        this.memo = memo;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public static CigaretteHistoryBatchIssueIgnoreResponse of(CigaretteHistoryBatchIssue issue) {
        return CigaretteHistoryBatchIssueIgnoreResponse.builder()
                .id(issue.getId())
                .targetDate(issue.getTargetDate())
                .userId(issue.getUserId())
                .status(issue.getStatus())
                .memo(issue.getMemo())
                .resolvedBy(issue.getResolvedBy())
                .resolvedAt(issue.getResolvedAt())
                .build();
    }
}
