package com.quitmate.challenge.missionhistory.service.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionHistoryUpdateServiceRequest {

    private Boolean isSuccess;

    @Builder
    public MissionHistoryUpdateServiceRequest(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
