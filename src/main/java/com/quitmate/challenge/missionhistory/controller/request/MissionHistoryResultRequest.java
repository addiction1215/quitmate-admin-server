package com.quitmate.challenge.missionhistory.controller.request;

import com.quitmate.challenge.missionhistory.service.request.MissionHistoryResultServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MissionHistoryResultRequest {

    @NotNull(message = "결과는 필수입니다.")
    private Boolean isSuccess;  // true: 성공(COMPLETED), false: 실패(FAILED)

    @Builder
    public MissionHistoryResultRequest(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public MissionHistoryResultServiceRequest toServiceRequest() {
        return MissionHistoryResultServiceRequest.builder()
                .isSuccess(isSuccess)
                .build();
    }
}
