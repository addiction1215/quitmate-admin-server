package com.quitmate.appMetrics.ga4.displayname.service.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Ga4DisplayNameSaveServiceRequest {

    private final String valueType;
    private final String rawValue;
    private final String displayName;
    private final String sourceType;

    @Builder
    public Ga4DisplayNameSaveServiceRequest(String valueType, String rawValue, String displayName, String sourceType) {
        this.valueType = valueType;
        this.rawValue = rawValue;
        this.displayName = displayName;
        this.sourceType = sourceType;
    }
}
