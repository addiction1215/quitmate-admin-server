package com.quitmate.smokingPattern.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SmokingPatternUserSnapshotListRequest extends PageInfoRequest {

    public SmokingPatternUserSnapshotListRequest(int page, int size) {
        super(page, size);
    }

    public PageInfoServiceRequest toServiceRequest() {
        return new PageInfoServiceRequest(getPage(), getSize());
    }
}
