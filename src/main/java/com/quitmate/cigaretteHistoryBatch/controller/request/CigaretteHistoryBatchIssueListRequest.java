package com.quitmate.cigaretteHistoryBatch.controller.request;

import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchIssueListServiceRequest;
import com.quitmate.global.page.request.PageInfoRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CigaretteHistoryBatchIssueListRequest extends PageInfoRequest {

    @Builder
    public CigaretteHistoryBatchIssueListRequest(int page, int size) {
        super(page, size);
    }

    public CigaretteHistoryBatchIssueListServiceRequest toServiceRequest() {
        return CigaretteHistoryBatchIssueListServiceRequest.builder()
                .page(getPage())
                .size(getSize())
                .build();
    }
}
