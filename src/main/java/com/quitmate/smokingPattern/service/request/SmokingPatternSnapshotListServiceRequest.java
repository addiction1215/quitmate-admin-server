package com.quitmate.smokingPattern.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SmokingPatternSnapshotListServiceRequest extends PageInfoServiceRequest {

    /** 분석 주 월요일(yyyy-MM-dd). 없으면 가장 최근 주 */
    private final String periodStart;
    /** SUFFICIENT·PARTIAL·INSUFFICIENT (선택) */
    private final String dataSufficiency;
    private final Long userId;
    /** 닉네임 또는 이메일 일부 (선택) */
    private final String keyword;

    @Builder
    private SmokingPatternSnapshotListServiceRequest(int page, int size, String periodStart, String dataSufficiency,
                                                     Long userId, String keyword) {
        super(page, size);
        this.periodStart = periodStart;
        this.dataSufficiency = dataSufficiency;
        this.userId = userId;
        this.keyword = keyword;
    }
}
