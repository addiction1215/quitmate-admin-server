package com.quitmate.challenge.missionhistory.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionHistoryResultServiceRequest {

    private Boolean isSuccess;

    @Builder
    public MissionHistoryResultServiceRequest(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
