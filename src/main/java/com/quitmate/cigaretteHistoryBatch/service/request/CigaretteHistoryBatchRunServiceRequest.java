package com.quitmate.cigaretteHistoryBatch.service.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CigaretteHistoryBatchRunServiceRequest {

    private final String targetDate;

    @Builder
    public CigaretteHistoryBatchRunServiceRequest(String targetDate) {
        this.targetDate = targetDate;
    }
}
