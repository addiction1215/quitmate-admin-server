package com.quitmate.cigaretteHistoryBatch.controller.request;

import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueIgnoreServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CigaretteHistoryBatchIssueIgnoreRequest {

    @NotBlank(message = "무시하는 이유(메모)는 필수입니다.")
    @Size(max = 500, message = "메모는 500자까지 쓸 수 있습니다.")
    private String memo;

    @Builder
    public CigaretteHistoryBatchIssueIgnoreRequest(String memo) {
        this.memo = memo;
    }

    public CigaretteHistoryBatchIssueIgnoreServiceRequest toServiceRequest(Long issueId) {
        return CigaretteHistoryBatchIssueIgnoreServiceRequest.builder()
                .issueId(issueId)
                .memo(memo)
                .build();
    }
}
