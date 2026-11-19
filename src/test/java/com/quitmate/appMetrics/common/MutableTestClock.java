package com.quitmate.appMetrics.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 테스트에서 시각을 앞으로 돌릴 수 있는 시계(앱 지표 캐시·기간 계산 확인용).
 *
 * <p>GA4(KAN-218)·푸시(KAN-221) 두 PR에 같은 내용으로 들어 있다.
 */
public class MutableTestClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableTestClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableTestClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
