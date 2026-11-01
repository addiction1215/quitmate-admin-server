package com.quitmate.faq.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FaqCategory {
    INFO_FRIEND("앱 사용법"),
    REPORT("통계"),
    LEADERBOARD("리더보드"),
    CHALLENGE("챌린지"),
    SMOKING_RECORD("흡연 기록");

    private final String description;
}
