package com.quitmate.notice.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.notice.enums.NoticeSearchCategory;
import com.quitmate.notice.enums.NoticeSortType;
import com.quitmate.notice.service.request.NoticeListServiceRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class NoticeListRequest extends PageInfoRequest {

    private NoticeSortType sortBy;          // 정렬 기준: CREATED_DATE 또는 TYPE (선택)
    private NoticeSearchCategory category;  // 검색 카테고리: CREATED_DATE, TYPE, CONTENT (선택)
    private String keyword;                 // 검색어 (선택)

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
