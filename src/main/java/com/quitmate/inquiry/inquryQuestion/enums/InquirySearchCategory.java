package com.quitmate.inquiry.inquryQuestion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquirySearchCategory {
    CREATED_DATE("createdDate", "작성일시"),
    TITLE("title", "문의 제목"),
    STATUS("status", "작성 상태");

    private final String value;
    private final String description;

    public static InquirySearchCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (InquirySearchCategory category : InquirySearchCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
}
