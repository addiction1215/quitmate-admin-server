package com.quitmate.appMetrics.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppMetricsCacheTest {

    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-11-19T13:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final AppMetricsCache<String> cache = new AppMetricsCache<>(Duration.ofHours(1), clock);
    private final AtomicInteger loads = new AtomicInteger();

    @DisplayName("1시간 안에는 담아 둔 값을 주고, 1시간이 되면 다시 불러온다")
    @Test
    void expiresAfterTtl() {
        assertThat(cache.get(AppMetricsPeriod.DAYS_28, this::load)).isEqualTo("값1");

        clock.advance(Duration.ofMinutes(59));
        assertThat(cache.get(AppMetricsPeriod.DAYS_28, this::load)).isEqualTo("값1");

        clock.advance(Duration.ofMinutes(1));
        assertThat(cache.get(AppMetricsPeriod.DAYS_28, this::load)).isEqualTo("값2");
        assertThat(loads).hasValue(2);
    }

    @DisplayName("기간마다 따로 담는다")
    @Test
    void perPeriod() {
        assertThat(cache.get(AppMetricsPeriod.DAYS_7, this::load)).isEqualTo("값1");
        assertThat(cache.get(AppMetricsPeriod.DAYS_28, this::load)).isEqualTo("값2");
        assertThat(cache.get(AppMetricsPeriod.DAYS_7, this::load)).isEqualTo("값1");
        assertThat(loads).hasValue(2);
    }

    @DisplayName("불러오다 실패하면 담지 않고, 다음 요청이 다시 불러온다")
    @Test
    void failureIsNotCached() {
        assertThatThrownBy(() -> cache.get(AppMetricsPeriod.DAYS_90, () -> {
            loads.incrementAndGet();
            throw new IllegalStateException("실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(cache.get(AppMetricsPeriod.DAYS_90, this::load)).isEqualTo("값2");
        assertThat(cache.get(AppMetricsPeriod.DAYS_90, this::load)).isEqualTo("값2");
        assertThat(loads).hasValue(2);
    }

    private String load() {
        return "값" + loads.incrementAndGet();
    }
}
