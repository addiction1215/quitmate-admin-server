package com.quitmate.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeSearchCategory {
    CREATED_DATE("createdDate", "작성일시"),
    TYPE("type", "유형"),
    CONTENT("content", "내용");

    private final String value;
    private final String description;

    public static NoticeSearchCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (NoticeSearchCategory category : NoticeSearchCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
}
