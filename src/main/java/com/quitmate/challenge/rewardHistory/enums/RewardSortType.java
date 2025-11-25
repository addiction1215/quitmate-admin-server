package com.quitmate.challenge.rewardHistory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardSortType {
    CREATED_DATE("createdDate", "발생일자"),
    USER_NAME("userName", "유저명");

    private final String value;
    private final String description;

    public static RewardSortType fromValue(String value) {
        if (value == null) {
            return CREATED_DATE;  // 기본값
        }
        
        for (RewardSortType type : RewardSortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return CREATED_DATE;  // 기본값
    }
}
