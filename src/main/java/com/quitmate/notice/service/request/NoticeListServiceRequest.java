package com.quitmate.notice.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.notice.enums.NoticeSearchCategory;
import com.quitmate.notice.enums.NoticeSortType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@SuperBuilder
public class NoticeListServiceRequest extends PageInfoServiceRequest {

    private NoticeSortType sortBy;
    private NoticeSearchCategory category;
    private String keyword;

    @Override
    public Pageable toPageable() {
        Sort sort = getSort();
        return PageRequest.of(getPage() - 1, getSize(), sort);
    }

    private Sort getSort() {
        NoticeSortType sortType = sortBy != null ? sortBy : NoticeSortType.CREATED_DATE;
        
        if (sortType == NoticeSortType.TYPE) {
            return Sort.by(Sort.Direction.ASC, "type");
        }
        // 기본값: 작성일시 내림차순
        return Sort.by(Sort.Direction.DESC, "createdDate");
    }
}
