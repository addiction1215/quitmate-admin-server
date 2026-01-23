package com.quitmate.challenge.challange.service.request;

import com.addiction.challenge.challange.controller.request.AdminChallengeSearchRequest;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Getter
public class AdminChallengeSearchServiceRequest {
    
    private final AdminChallengeSearchRequest.SearchCategory category;
    private final String keyword;
    private final AdminChallengeSearchRequest.SortType sortType;
    private final Pageable pageable;
    
    @Builder
    public AdminChallengeSearchServiceRequest(
            AdminChallengeSearchRequest.SearchCategory category,
            String keyword,
            AdminChallengeSearchRequest.SortType sortType,
            Pageable pageable) {
        this.category = category;
        this.keyword = keyword;
        this.sortType = sortType;
        this.pageable = pageable;
    }
}
