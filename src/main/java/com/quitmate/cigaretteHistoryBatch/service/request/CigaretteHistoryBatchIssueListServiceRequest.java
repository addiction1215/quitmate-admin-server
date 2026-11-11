package com.quitmate.cigaretteHistoryBatch.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
public class CigaretteHistoryBatchIssueListServiceRequest extends PageInfoServiceRequest {

    @Builder
    public CigaretteHistoryBatchIssueListServiceRequest(int page, int size) {
        super(page, size);
    }

    /**
     * 오래된 날짜부터, 같은 날짜는 사용자 ID 순으로 보여 준다.
     */
    @Override
    public Pageable toPageable() {
        return PageRequest.of(getPage() - 1, getSize(), Sort.by("targetDate", "userId", "id"));
    }
}
