package com.quitmate.appMetrics.push.service;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.common.MutableTestClock;
import com.quitmate.appMetrics.push.repository.PushMetricsRepository;
import com.quitmate.appMetrics.push.repository.response.DeliveryDailyCount;
import com.quitmate.appMetrics.push.repository.response.ErrorCodeCount;
import com.quitmate.appMetrics.push.repository.response.OutboxDailyCount;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Daily;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.ErrorCode;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Summary;
import com.quitmate.global.exception.QuitmateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PushMetricsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-11-19 22:10 KST */
    private static final Instant NOW = Instant.parse("2026-11-19T13:10:00Z");
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 13, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 20, 0, 0);

    private final PushMetricsRepository repository = mock(PushMetricsRepository.class);
    private final MutableTestClock clock = new MutableTestClock(NOW, KST);
    private final PushMetricsService service = new PushMetricsService(repository, clock);

    @DisplayName("전달·열림 기록이 없으면(KAN-219·220 배포 전) ticket·receipt·opened·openRate는 null, 발송 요청·토큰 회원은 숫자")
    @Test
    void notTracked() {
        // given
        given(repository.findOutboxDailyCounts(FROM, TO)).willReturn(List.of(
                new OutboxDailyCount(LocalDate.of(2026, 11, 13), 100, 98, 0),
                new OutboxDailyCount(LocalDate.of(2026, 11, 19), 120, 120, 0)));
        given(repository.findDeliveryDailyCounts(FROM, TO)).willReturn(List.of());
        given(repository.countTokenUsers()).willReturn(181L);

        // when
        PushMetricsResponse response = service.getMetrics("7");

        // then
        assertThat(response.period()).isEqualTo(7);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(response.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T22:10:00+09:00"));
        assertThat(response.deliveryTracking()).isFalse();
        assertThat(response.openTracking()).isFalse();
        assertThat(response.summary()).isEqualTo(new Summary(220, null, null, null, null, null, null, null, 181));
        assertThat(response.daily()).hasSize(7);
        assertThat(response.daily().get(0)).isEqualTo(new Daily(LocalDate.of(2026, 11, 13), 100, null, null, null));
        assertThat(response.daily().get(1)).isEqualTo(new Daily(LocalDate.of(2026, 11, 14), 0, null, null, null));
        assertThat(response.daily().get(6)).isEqualTo(new Daily(LocalDate.of(2026, 11, 19), 120, null, null, null));
        assertThat(response.errorCodes()).isEmpty();
        verify(repository, never()).findErrorCodeCounts(any(), any());
    }

    @DisplayName("전달·열림 기록이 있으면 숫자로 채운다. 기록 없는 날은 0, 발송 요청에 outbox 없는 전달 행을 더한다")
    @Test
    void tracked() {
        given(repository.findOutboxDailyCounts(FROM, TO)).willReturn(List.of(
                new OutboxDailyCount(LocalDate.of(2026, 11, 13), 100, 90, 30),
                new OutboxDailyCount(LocalDate.of(2026, 11, 18), 50, 30, 0)));
        given(repository.findDeliveryDailyCounts(FROM, TO)).willReturn(List.of(
                new DeliveryDailyCount(LocalDate.of(2026, 11, 18), 70, 20, 65, 5, 50, 3, 12),
                new DeliveryDailyCount(LocalDate.of(2026, 11, 19), 10, 10, 10, 0, 0, 0, 10)));
        given(repository.findErrorCodeCounts(FROM, TO)).willReturn(List.of(
                new ErrorCodeCount("DeviceNotRegistered", 6), new ErrorCodeCount("MessageRateExceeded", 2)));
        given(repository.countTokenUsers()).willReturn(181L);

        PushMetricsResponse response = service.getMetrics("7");

        assertThat(response.deliveryTracking()).isTrue();
        assertThat(response.openTracking()).isTrue();
        // requested = outbox 150 + outbox 없는 전달 30, openRate = 30 / (90 + 30)
        assertThat(response.summary()).isEqualTo(new Summary(180, 75L, 5L, 50L, 3L, 22L, 30L, 0.25, 181));
        assertThat(response.daily()).containsExactly(
                new Daily(LocalDate.of(2026, 11, 13), 100, 0L, 0L, 30L),
                new Daily(LocalDate.of(2026, 11, 14), 0, 0L, 0L, 0L),
                new Daily(LocalDate.of(2026, 11, 15), 0, 0L, 0L, 0L),
                new Daily(LocalDate.of(2026, 11, 16), 0, 0L, 0L, 0L),
                new Daily(LocalDate.of(2026, 11, 17), 0, 0L, 0L, 0L),
                new Daily(LocalDate.of(2026, 11, 18), 70, 5L, 3L, 0L),
                new Daily(LocalDate.of(2026, 11, 19), 10, 0L, 0L, 0L));
        assertThat(response.errorCodes()).containsExactly(
                new ErrorCode("DeviceNotRegistered", 6), new ErrorCode("MessageRateExceeded", 2));
    }

    @DisplayName("열림률은 0~1 소수 넷째 자리(반올림)")
    @Test
    void openRateIsRounded() {
        given(repository.findOutboxDailyCounts(FROM, TO)).willReturn(List.of(
                new OutboxDailyCount(LocalDate.of(2026, 11, 13), 3, 3, 1)));
        given(repository.findDeliveryDailyCounts(FROM, TO)).willReturn(List.of());

        PushMetricsResponse response = service.getMetrics("7");

        assertThat(response.openTracking()).isTrue();
        assertThat(response.deliveryTracking()).isFalse();
        assertThat(response.summary().opened()).isEqualTo(1L);
        assertThat(response.summary().openRate()).isEqualTo(0.3333);
        assertThat(response.daily().get(0).opened()).isEqualTo(1L);
        assertThat(response.daily().get(0).ticketError()).isNull();
    }

    @DisplayName("끝 날짜는 오늘(한국 날짜). 한국 자정을 넘기면 다음 날이 끝 날짜다. 28·90일도 period개")
    @Test
    void periodRange() {
        clock.advance(Duration.ofHours(2)); // 2026-11-20 00:10 KST
        LocalDateTime to = LocalDateTime.of(2026, 11, 21, 0, 0);
        given(repository.findOutboxDailyCounts(any(), any())).willReturn(List.of());
        given(repository.findDeliveryDailyCounts(any(), any())).willReturn(List.of());

        PushMetricsResponse days28 = service.getMetrics("28");
        PushMetricsResponse days90 = service.getMetrics("90");

        assertThat(days28.endDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(days28.startDate()).isEqualTo(LocalDate.of(2026, 10, 24));
        assertThat(days28.daily()).hasSize(28);
        assertThat(days90.startDate()).isEqualTo(LocalDate.of(2026, 8, 23));
        assertThat(days90.daily()).hasSize(90);
        verify(repository).findOutboxDailyCounts(LocalDateTime.of(2026, 10, 24, 0, 0), to);
        verify(repository).findOutboxDailyCounts(LocalDateTime.of(2026, 8, 23, 0, 0), to);
    }

    @DisplayName("기간별로 1시간 캐시한다. fetchedAt은 DB를 읽은 시각 그대로")
    @Test
    void cachesForOneHour() {
        given(repository.findOutboxDailyCounts(any(), any())).willReturn(List.of());
        given(repository.findDeliveryDailyCounts(any(), any())).willReturn(List.of());

        PushMetricsResponse first = service.getMetrics("7");
        clock.advance(Duration.ofMinutes(59));
        assertThat(service.getMetrics("7")).isSameAs(first);
        verify(repository, times(1)).countTokenUsers();

        service.getMetrics("28");
        verify(repository, times(2)).countTokenUsers();

        clock.advance(Duration.ofMinutes(1));
        PushMetricsResponse refreshed = service.getMetrics("7");
        assertThat(refreshed.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T23:10:00+09:00"));
        verify(repository, times(3)).countTokenUsers();
    }

    @DisplayName("period가 7·28·90이 아니면 400이고 DB를 읽지 않는다")
    @Test
    void invalidPeriod() {
        assertThatThrownBy(() -> service.getMetrics("14"))
                .isInstanceOf(QuitmateException.class)
                .hasMessage(AppMetricsPeriod.INVALID_PERIOD);
        assertThatThrownBy(() -> service.getMetrics(null)).isInstanceOf(QuitmateException.class);
        verifyNoInteractions(repository);
    }
}
