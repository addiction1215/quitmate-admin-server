package com.quitmate.appMetrics.ga4.service;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.common.MutableTestClock;
import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4Report.Row;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.global.exception.QuitmateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class Ga4MetricsServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-11-19 22:10 KST */
    private static final Instant NOW = Instant.parse("2026-11-19T13:10:00Z");
    private static final Ga4Properties CONNECTED = new Ga4Properties("123456789", "/srv/ga4-key.json");

    private final Ga4DataApiClient client = mock(Ga4DataApiClient.class);
    private final MutableTestClock clock = new MutableTestClock(NOW, KST);

    @DisplayName("속성 ID·키 파일 경로 중 하나라도 없거나 속성 ID가 숫자가 아니면 GA4를 부르지 않고 enabled=false")
    @Test
    void disabledWhenNotConfigured() {
        List<Ga4Properties> notConfigured = List.of(
                new Ga4Properties(null, null),
                new Ga4Properties("123456789", null),
                new Ga4Properties(null, "/srv/ga4-key.json"),
                new Ga4Properties(" ", "/srv/ga4-key.json"),
                new Ga4Properties("null", "null"),
                new Ga4Properties("G-ABC123", "/srv/ga4-key.json"));

        for (Ga4Properties properties : notConfigured) {
            Ga4MetricsResponse response = service(properties).getMetrics("28");

            assertThat(response.enabled()).isFalse();
            assertThat(response.period()).isEqualTo(28);
            assertThat(response.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T22:10:00+09:00"));
            assertThat(response.startDate()).isNull();
            assertThat(response.endDate()).isNull();
            assertThat(response.timeZone()).isNull();
            assertThat(response.consoleUrl()).isNull();
            assertThat(response.summary()).isNull();
            assertThat(response.dailyActiveUsers()).isEmpty();
            assertThat(response.topScreens()).isEmpty();
            assertThat(response.platforms()).isEmpty();
            assertThat(response.appVersions()).isEmpty();
        }
        verify(client, never()).runReport(anyString(), any());
    }

    @DisplayName("period가 7·28·90이 아니면 연결 여부와 관계없이 400")
    @Test
    void invalidPeriod() {
        assertThatThrownBy(() -> service(new Ga4Properties(null, null)).getMetrics("30"))
                .isInstanceOf(QuitmateException.class)
                .hasMessage(AppMetricsPeriod.INVALID_PERIOD);
        assertThatThrownBy(() -> service(CONNECTED).getMetrics(null))
                .isInstanceOf(QuitmateException.class);
        verify(client, never()).runReport(anyString(), any());
    }

    @DisplayName("GA4를 다섯 번 불러 요약·날짜별·화면·플랫폼·앱 버전을 묶는다. 기간은 GA4 시간대 기준 어제까지 28일")
    @Test
    void connected() {
        // given
        answerWith("Asia/Seoul");

        // when
        Ga4MetricsResponse response = service(CONNECTED).getMetrics("28");

        // then
        assertThat(response.enabled()).isTrue();
        assertThat(response.period()).isEqualTo(28);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(response.timeZone()).isEqualTo("Asia/Seoul");
        assertThat(response.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T22:10:00+09:00"));
        assertThat(response.consoleUrl())
                .isEqualTo("https://analytics.google.com/analytics/web/#/p123456789/reports/intelligenthome");
        assertThat(response.summary()).isEqualTo(new Ga4MetricsResponse.Summary(120, 410, 905, 85, 1002));

        // 빠진 날은 0, 기간 밖(2026-10-21) 행은 버림, 날짜 오름차순 28개
        assertThat(response.dailyActiveUsers()).hasSize(28);
        assertThat(response.dailyActiveUsers().get(0))
                .isEqualTo(new Ga4MetricsResponse.DailyActiveUsers(LocalDate.of(2026, 10, 22), 118));
        assertThat(response.dailyActiveUsers().get(1))
                .isEqualTo(new Ga4MetricsResponse.DailyActiveUsers(LocalDate.of(2026, 10, 23), 0));
        assertThat(response.dailyActiveUsers().get(27))
                .isEqualTo(new Ga4MetricsResponse.DailyActiveUsers(LocalDate.of(2026, 11, 18), 97));
        assertThat(response.dailyActiveUsers()).extracting(Ga4MetricsResponse.DailyActiveUsers::date)
                .isSorted().doesNotContain(LocalDate.of(2026, 10, 21));

        // 화면은 조회 수 내림차순 10개
        assertThat(response.topScreens()).hasSize(10);
        assertThat(response.topScreens().get(0)).isEqualTo(new Ga4MetricsResponse.Screen("Screen12", 1200));
        assertThat(response.topScreens()).extracting(Ga4MetricsResponse.Screen::views)
                .isSortedAccordingTo((a, b) -> Long.compare(b, a));
        assertThat(response.platforms()).extracting(Ga4MetricsResponse.Platform::platform, Ga4MetricsResponse.Platform::activeUsers)
                .containsExactly(tuple("iOS", 600L), tuple("Android", 402L));
        assertThat(response.appVersions()).hasSize(10);
        assertThat(response.appVersions().get(0)).isEqualTo(new Ga4MetricsResponse.AppVersion("1.1.11", 1100));

        // 보낸 요청: 상대 날짜, 지표·차원 이름
        List<Ga4ReportRequest> requests = sentRequests(5);
        assertThat(requests).extracting(Ga4ReportRequest::name)
                .containsExactly("summary", "dailyActiveUsers", "topScreens", "platforms", "appVersions");
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.startDate()).isEqualTo("28daysAgo");
            assertThat(request.endDate()).isEqualTo("yesterday");
        });
        assertThat(requests.get(0).dimensions()).isEmpty();
        assertThat(requests.get(0).metrics())
                .containsExactly("active1DayUsers", "active7DayUsers", "active28DayUsers", "newUsers", "activeUsers");
        assertThat(requests.get(1).dimensions()).containsExactly("date");
        assertThat(requests.get(1).metrics()).containsExactly("activeUsers");
        assertThat(requests.get(2).dimensions()).containsExactly("unifiedScreenName");
        assertThat(requests.get(2).metrics()).containsExactly("screenPageViews");
        assertThat(requests.get(2).orderByMetric()).isEqualTo("screenPageViews");
        assertThat(requests.get(2).limit()).isEqualTo(10);
        assertThat(requests.get(3).dimensions()).containsExactly("platform");
        assertThat(requests.get(4).dimensions()).containsExactly("appVersion");
        assertThat(requests.get(4).limit()).isEqualTo(10);
    }

    @DisplayName("GA4 속성 시간대가 한국과 다르면 그 시간대의 어제가 끝 날짜다. fetchedAt은 한국 시각")
    @Test
    void propertyTimeZone() {
        // 2026-11-19 11:00 KST = 2026-11-18 18:00 LA
        clock.advance(Duration.between(NOW, Instant.parse("2026-11-19T02:00:00Z")));
        answerWith("America/Los_Angeles");

        Ga4MetricsResponse response = service(CONNECTED).getMetrics("7");

        assertThat(response.timeZone()).isEqualTo("America/Los_Angeles");
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(response.dailyActiveUsers()).hasSize(7);
        assertThat(response.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T11:00:00+09:00"));
        assertThat(sentRequests(5)).allSatisfy(request -> assertThat(request.startDate()).isEqualTo("7daysAgo"));
    }

    @DisplayName("응답에 시간대가 없으면 한국 시간, 데이터가 없으면 요약 0·날짜별 0")
    @Test
    void emptyReports() {
        given(client.runReport(eq("123456789"), any())).willReturn(new Ga4Report(null, List.of()));

        Ga4MetricsResponse response = service(new Ga4Properties("properties/123456789", "/srv/ga4-key.json"))
                .getMetrics("90");

        assertThat(response.enabled()).isTrue();
        assertThat(response.timeZone()).isEqualTo("Asia/Seoul");
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(response.summary()).isEqualTo(new Ga4MetricsResponse.Summary(0, 0, 0, 0, 0));
        assertThat(response.dailyActiveUsers()).hasSize(90)
                .allSatisfy(day -> assertThat(day.activeUsers()).isZero());
        assertThat(response.topScreens()).isEmpty();
        assertThat(response.platforms()).isEmpty();
        assertThat(response.appVersions()).isEmpty();
    }

    @DisplayName("성공한 결과는 기간별로 1시간 캐시한다. fetchedAt은 GA4를 부른 시각 그대로")
    @Test
    void cachesSuccessForOneHour() {
        answerWith("Asia/Seoul");
        Ga4MetricsService service = service(CONNECTED);

        Ga4MetricsResponse first = service.getMetrics("28");
        clock.advance(Duration.ofMinutes(59));
        Ga4MetricsResponse cached = service.getMetrics("28");

        assertThat(cached).isSameAs(first);
        assertThat(cached.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T22:10:00+09:00"));
        verify(client, times(5)).runReport(eq("123456789"), any());

        service.getMetrics("7");
        verify(client, times(10)).runReport(eq("123456789"), any());

        clock.advance(Duration.ofMinutes(1));
        Ga4MetricsResponse refreshed = service.getMetrics("28");
        assertThat(refreshed.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T23:10:00+09:00"));
        verify(client, times(15)).runReport(eq("123456789"), any());
    }

    @DisplayName("GA4 호출이 하나라도 실패하면 503 예외를 그대로 던지고 캐시하지 않는다")
    @Test
    void failureIsNotCached() {
        AtomicInteger calls = new AtomicInteger();
        given(client.runReport(eq("123456789"), any())).willAnswer(invocation -> {
            if (calls.incrementAndGet() == 3) {
                throw new Ga4UnavailableException("GA4 오류 응답 429 RESOURCE_EXHAUSTED(topScreens).");
            }
            return report(invocation.getArgument(1), "Asia/Seoul");
        });
        Ga4MetricsService service = service(CONNECTED);

        assertThatThrownBy(() -> service.getMetrics("28"))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("RESOURCE_EXHAUSTED");

        Ga4MetricsResponse response = service.getMetrics("28");
        assertThat(response.enabled()).isTrue();
        assertThat(calls).hasValue(3 + 5);
    }

    @DisplayName("날짜 값이 YYYYMMDD가 아니면 응답 이상으로 503")
    @Test
    void malformedDate() {
        given(client.runReport(eq("123456789"), any())).willAnswer(invocation -> {
            Ga4ReportRequest request = invocation.getArgument(1);
            if (request.dimensions().contains("date")) {
                return new Ga4Report("Asia/Seoul", List.of(new Row(List.of("2026-11-18"), List.of(1L))));
            }
            return new Ga4Report("Asia/Seoul", List.of());
        });

        assertThatThrownBy(() -> service(CONNECTED).getMetrics("7"))
                .isInstanceOf(Ga4UnavailableException.class)
                .hasMessageContaining("날짜");
    }

    private Ga4MetricsService service(Ga4Properties properties) {
        return new Ga4MetricsService(properties, client, clock);
    }

    private void answerWith(String timeZone) {
        given(client.runReport(eq("123456789"), any()))
                .willAnswer(invocation -> report(invocation.getArgument(1), timeZone));
    }

    private List<Ga4ReportRequest> sentRequests(int count) {
        ArgumentCaptor<Ga4ReportRequest> captor = ArgumentCaptor.forClass(Ga4ReportRequest.class);
        verify(client, times(count)).runReport(eq("123456789"), captor.capture());
        return captor.getAllValues();
    }

    /** 요청 이름에 맞춘 가짜 GA4 보고서. 정렬은 일부러 섞어 둔다. */
    private static Ga4Report report(Ga4ReportRequest request, String timeZone) {
        List<Row> rows = new ArrayList<>();
        switch (request.name()) {
            case "summary" -> rows.add(new Row(List.of(), List.of(120L, 410L, 905L, 85L, 1002L)));
            case "dailyActiveUsers" -> {
                rows.add(new Row(List.of("20261118"), List.of(97L)));
                rows.add(new Row(List.of("20261022"), List.of(118L)));
                rows.add(new Row(List.of("20261021"), List.of(999L)));
                rows.add(new Row(List.of("20261111"), List.of(80L)));
            }
            case "topScreens" -> {
                for (int i = 1; i <= 12; i++) {
                    rows.add(new Row(List.of("Screen" + i), List.of(i * 100L)));
                }
            }
            case "platforms" -> {
                rows.add(new Row(List.of("Android"), List.of(402L)));
                rows.add(new Row(List.of("iOS"), List.of(600L)));
            }
            case "appVersions" -> {
                for (int i = 0; i <= 11; i++) {
                    rows.add(new Row(List.of("1.1." + i), List.of(i * 100L)));
                }
            }
            default -> throw new IllegalArgumentException(request.name());
        }
        return new Ga4Report(timeZone, rows);
    }
}
