package com.quitmate.cigaretteHistoryBatch.service.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CigaretteHistoryBatchIssueIgnoreServiceRequest {

    private final Long issueId;
    private final String memo;

    @Builder
    public CigaretteHistoryBatchIssueIgnoreServiceRequest(Long issueId, String memo) {
        this.issueId = issueId;
        this.memo = memo;
    }
}
