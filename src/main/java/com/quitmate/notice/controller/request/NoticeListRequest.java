package com.quitmate.notice.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.notice.enums.NoticeSearchCategory;
import com.quitmate.notice.enums.NoticeSortType;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NoticeListRequest extends PageInfoRequest {

    private final NoticeSortType sortBy;          // 정렬 기준: CREATED_DATE 또는 TYPE (선택)
    private final NoticeSearchCategory category;  // 검색 카테고리: CREATED_DATE, TYPE, CONTENT (선택)
    private final String keyword;                 // 검색어 (선택)

    @Builder
    public NoticeListRequest(int page, int size, NoticeSearchCategory category, NoticeSortType sortBy, String keyword) {
        super(page, size);
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
    }

    public NoticeListServiceRequest toServiceRequest() {
        return NoticeListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
