package com.quitmate.challenge.rewardHistory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardSearchCategory {
    CREATED_DATE("createdDate", "발생일자"),
    USER_NAME("userName", "유저명"),
    TYPE("type", "유형");

    private final String value;
    private final String description;

    public static RewardSearchCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (RewardSearchCategory category : RewardSearchCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
}
