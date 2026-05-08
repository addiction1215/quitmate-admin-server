package com.quitmate.faq.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FaqCategory {
    INFO_FRIEND("정보 및 친구"),
    REPORT("리포트"),
    LEADERBOARD("리더보드"),
    CHALLENGE("챌린지");

    private final String description;
}
