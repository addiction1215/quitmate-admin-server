package com.quitmate.challenge.rewardHistory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardType {
    USED("사용"), // 사용
    ACQUIRE("획득"); // 획득

    private final String description;
}
