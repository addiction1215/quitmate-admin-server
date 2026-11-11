package com.quitmate.cigaretteHistoryBatch.controller.request;

import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchRunServiceRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CigaretteHistoryBatchRunRequest {

    @NotBlank(message = "이관할 날짜는 필수입니다.")
    private String targetDate;

    @Builder
    public CigaretteHistoryBatchRunRequest(String targetDate) {
        this.targetDate = targetDate;
    }

    public CigaretteHistoryBatchRunServiceRequest toServiceRequest() {
        return CigaretteHistoryBatchRunServiceRequest.builder()
                .targetDate(targetDate)
                .build();
    }
}
