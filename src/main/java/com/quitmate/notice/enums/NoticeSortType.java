package com.quitmate.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeSortType {
    CREATED_DATE("createdDate", "작성일시"),
    TYPE("type", "유형");

    private final String value;
    private final String description;

    public static NoticeSortType fromValue(String value) {
        if (value == null) {
            return CREATED_DATE;  // 기본값
        }
        
        for (NoticeSortType type : NoticeSortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return CREATED_DATE;  // 기본값
    }
}
