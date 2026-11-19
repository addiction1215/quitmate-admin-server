package com.quitmate.appMetrics.common;

import com.quitmate.global.exception.QuitmateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppMetricsPeriodTest {

    @DisplayName("7·28·90은 그 기간으로 읽고, 앞뒤 공백은 무시한다")
    @Test
    void from() {
        assertThat(AppMetricsPeriod.from("7")).isEqualTo(AppMetricsPeriod.DAYS_7);
        assertThat(AppMetricsPeriod.from("28")).isEqualTo(AppMetricsPeriod.DAYS_28);
        assertThat(AppMetricsPeriod.from(" 90 ")).isEqualTo(AppMetricsPeriod.DAYS_90);
    }

    @DisplayName("그 밖의 값·빈 값은 400(QuitmateException)이다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "0", "1", "30", "-7", "07", "7.0", "abc", "28days"})
    void invalid(String value) {
        assertThatThrownBy(() -> AppMetricsPeriod.from(value))
                .isInstanceOf(QuitmateException.class)
                .hasMessage(AppMetricsPeriod.INVALID_PERIOD);
    }

    @DisplayName("시작 날짜는 끝 날짜를 포함해 기간 일수가 되는 날이다")
    @Test
    void startDate() {
        LocalDate end = LocalDate.of(2026, 11, 18);

        assertThat(AppMetricsPeriod.DAYS_7.startDate(end)).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(AppMetricsPeriod.DAYS_28.startDate(end)).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(AppMetricsPeriod.DAYS_90.startDate(end)).isEqualTo(LocalDate.of(2026, 8, 21));
    }
}
