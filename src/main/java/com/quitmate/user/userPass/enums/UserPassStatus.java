package com.quitmate.user.userPass.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이용권 상태. DB에 저장하지 않고 조회 시각 기준으로 계산한다.
 * 기간은 [starts_at, ends_at) — 시작 시각은 포함, 만료 시각은 포함하지 않는다.
 */
@Getter
@RequiredArgsConstructor
public enum UserPassStatus {
    ACTIVE("이용 중"),
    SCHEDULED("시작 전"),
    EXPIRED("만료"),
    REVOKED("회수");

    private final String description;

    public static UserPassStatus of(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime revokedAt,
                                    LocalDateTime now) {
        if (revokedAt != null) {
            return REVOKED;
        }
        if (!endsAt.isAfter(now)) {
            return EXPIRED;
        }
        if (startsAt.isAfter(now)) {
            return SCHEDULED;
        }
        return ACTIVE;
    }
}
