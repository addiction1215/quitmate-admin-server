package com.quitmate.cigaretteHistoryBatch.service.response;

import com.quitmate.cigaretteHistoryBatch.entity.CigaretteHistoryBatchIssue;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 「이관 처리 대기」 한 줄. 사용자 ID와 건수·예외 타입만 담고, 개인정보와 흡연 상세는 넣지 않는다.
 */
@Getter
public class CigaretteHistoryBatchIssueResponse {

    private final Long id;
    private final String targetDate;
    private final Long userId;
    private final CigaretteHistoryBatchIssueStage stage;
    private final int failCount;
    private final String lastErrorType;
    private final LocalDateTime firstFailedAt;
    private final LocalDateTime lastFailedAt;
    private final String memo;

    @Builder
    public CigaretteHistoryBatchIssueResponse(Long id, String targetDate, Long userId,
                                              CigaretteHistoryBatchIssueStage stage, int failCount,
                                              String lastErrorType, LocalDateTime firstFailedAt,
                                              LocalDateTime lastFailedAt, String memo) {
        this.id = id;
        this.targetDate = targetDate;
        this.userId = userId;
        this.stage = stage;
        this.failCount = failCount;
        this.lastErrorType = lastErrorType;
        this.firstFailedAt = firstFailedAt;
        this.lastFailedAt = lastFailedAt;
        this.memo = memo;
    }

    public static CigaretteHistoryBatchIssueResponse of(CigaretteHistoryBatchIssue issue) {
        return CigaretteHistoryBatchIssueResponse.builder()
                .id(issue.getId())
                .targetDate(issue.getTargetDate())
                .userId(issue.getUserId())
                .stage(issue.getStage())
                .failCount(issue.getFailCount())
                .lastErrorType(issue.getLastErrorType())
                .firstFailedAt(issue.getFirstFailedAt())
                .lastFailedAt(issue.getLastFailedAt())
                .memo(issue.getMemo())
                .build();
    }
}
