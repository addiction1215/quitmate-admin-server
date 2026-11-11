package com.quitmate.smokingPattern.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.smokingPattern.service.request.SmokingPatternSnapshotListServiceRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SmokingPatternSnapshotListRequest extends PageInfoRequest {

    private String periodStart;
    private String dataSufficiency;
    private Long userId;
    private String keyword;

    public SmokingPatternSnapshotListServiceRequest toServiceRequest() {
        return SmokingPatternSnapshotListServiceRequest.builder()
                .page(getPage())
                .size(getSize())
                .periodStart(periodStart)
                .dataSufficiency(dataSufficiency)
                .userId(userId)
                .keyword(keyword)
                .build();
    }
}
