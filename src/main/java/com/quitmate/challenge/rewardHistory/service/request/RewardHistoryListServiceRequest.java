package com.quitmate.challenge.rewardHistory.service.request;

import com.quitmate.challenge.rewardHistory.enums.RewardSearchCategory;
import com.quitmate.challenge.rewardHistory.enums.RewardSortType;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@SuperBuilder
public class RewardHistoryListServiceRequest extends PageInfoServiceRequest {

    private RewardSortType sortBy;
    private RewardSearchCategory category;
    private String keyword;

    @Override
    public Pageable toPageable() {
        Sort sort = getSort();
        return PageRequest.of(getPage() - 1, getSize(), sort);
    }

    private Sort getSort() {
        RewardSortType sortType = sortBy != null ? sortBy : RewardSortType.CREATED_DATE;
        
        if (sortType == RewardSortType.USER_NAME) {
            return Sort.by(Sort.Direction.ASC, "user.nickName");
        }
        // 기본값: 발생일자 내림차순
        return Sort.by(Sort.Direction.DESC, "createdDate");
    }
}
