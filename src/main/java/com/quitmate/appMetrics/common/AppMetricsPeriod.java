package com.quitmate.appMetrics.common;

import com.quitmate.global.exception.QuitmateException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 「앱 지표」 화면(KAN-217)의 조회 기간. 7·28·90일만 받고, 그 밖의 값·빈 값은 400이다.
 *
 * <p>GA4(KAN-218)·푸시(KAN-221) 두 PR에 같은 내용으로 들어 있다. 어느 PR이 먼저 머지돼도 충돌하지 않도록
 * 이 파일은 두 브랜치에서 똑같이 둔다.
 */
public enum AppMetricsPeriod {

    DAYS_7(7),
    DAYS_28(28),
    DAYS_90(90);

    public static final String INVALID_PERIOD = "조회 기간(period)은 7, 28, 90 중 하나로 보내 주세요.";

    private final int days;

    AppMetricsPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    /** 끝 날짜를 포함해 {@link #days()}일이 되는 시작 날짜 */
    public LocalDate startDate(LocalDate endDate) {
        return endDate.minusDays(days - 1L);
    }

    /**
     * @param value 쿼리 문자열 그대로(예: "28")
     * @throws QuitmateException 7·28·90이 아니면(400)
     */
    public static AppMetricsPeriod from(String value) {
        if (!StringUtils.hasText(value)) {
            throw new QuitmateException(INVALID_PERIOD);
        }
        String trimmed = value.trim();
        for (AppMetricsPeriod period : values()) {
            if (String.valueOf(period.days).equals(trimmed)) {
                return period;
            }
        }
        throw new QuitmateException(INVALID_PERIOD);
    }
}
