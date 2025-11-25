package com.quitmate.inquiry.inquryQuestion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquirySortType {
    CREATED_DATE("createdDate", "작성일시"),
    STATUS("status", "작성자");

    private final String value;
    private final String description;

    public static InquirySortType fromValue(String value) {
        if (value == null) {
            return CREATED_DATE;  // 기본값
        }
        
        for (InquirySortType type : InquirySortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return CREATED_DATE;  // 기본값
    }
}
