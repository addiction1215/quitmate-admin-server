package com.quitmate.challenge.missionhistory.controller.request;

import com.quitmate.challenge.missionhistory.service.request.MissionHistoryUpdateServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionHistoryUpdateRequest {

    @NotNull(message = "결과 심사는 필수입니다.")
    private Boolean isSuccess;

    @Builder
    public MissionHistoryUpdateRequest(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public MissionHistoryUpdateServiceRequest toServiceRequest() {
        return MissionHistoryUpdateServiceRequest.builder()
                .isSuccess(isSuccess)
                .build();
    }
}
