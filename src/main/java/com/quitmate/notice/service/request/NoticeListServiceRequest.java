package com.quitmate.notice.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.notice.enums.NoticeSearchCategory;
import com.quitmate.notice.enums.NoticeSortType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
public class NoticeListServiceRequest extends PageInfoServiceRequest {

    private final NoticeSortType sortBy;
    private final NoticeSearchCategory category;
    private final String keyword;

    @Builder
    public NoticeListServiceRequest(int page, int size, NoticeSearchCategory category, String keyword, NoticeSortType sortBy) {
        super(page, size);
        this.category = category;
        this.keyword = keyword;
        this.sortBy = sortBy;
    }
}
