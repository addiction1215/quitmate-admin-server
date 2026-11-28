package com.quitmate.appMetrics.ga4.service;

import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.common.MutableTestClock;
import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4Report.Row;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNames;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUserCounts;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUsersByPlatform;
import com.quitmate.appMetrics.ga4.service.response.Ga4RecentValues;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
            assertThat(response.activeUsersByPlatform()).isNull();
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

    @DisplayName("GA4를 여덟 번 불러 요약·날짜별·화면·플랫폼·앱 버전·플랫폼별 활성 사용자를 묶는다. 기간은 GA4 시간대 기준 어제까지 28일")
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
        // DAU·WAU·MAU는 어제 하루 범위(summary), 신규·기간 전체 활성은 기간 범위(periodTotals)에서 온다
        assertThat(response.summary()).isEqualTo(new Ga4MetricsResponse.Summary(140, 470, 1100, 85, 1002));
        assertThat(response.activeUsersByPlatform()).isEqualTo(new ActiveUsersByPlatform(
                new ActiveUserCounts(120, 400, 950),
                new ActiveUserCounts(50, 200, 500),
                new ActiveUserCounts(80, 230, 520),
                new ActiveUserCounts(30, 90, 200)));

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

        // 화면은 조회 수 내림차순 10개. 이름표에 없고 괄호로 시작하지 않는 화면은 웹 페이지로 본다
        assertThat(response.topScreens()).hasSize(10);
        assertThat(response.topScreens().get(0)).isEqualTo(
                new Ga4MetricsResponse.Screen("Screen12", 1200, "[웹] Screen12", Ga4SourceType.WEB));
        assertThat(response.topScreens()).extracting(Ga4MetricsResponse.Screen::views)
                .isSortedAccordingTo((a, b) -> Long.compare(b, a));
        assertThat(response.platforms())
                .extracting(Ga4MetricsResponse.Platform::platform, Ga4MetricsResponse.Platform::activeUsers,
                        Ga4MetricsResponse.Platform::displayName)
                .containsExactly(tuple("iOS", 600L, "앱(iOS)"), tuple("Android", 402L, "앱(Android)"));
        assertThat(response.appVersions()).hasSize(10);
        assertThat(response.appVersions().get(0)).isEqualTo(new Ga4MetricsResponse.AppVersion("1.1.11", 1100, "1.1.11"));

        // 보낸 요청: 상대 날짜, 지표·차원 이름
        List<Ga4ReportRequest> requests = sentRequests(8);
        assertThat(requests).extracting(Ga4ReportRequest::name)
                .containsExactly("summary", "periodTotals", "dailyActiveUsers", "topScreens", "platforms", "appVersions",
                        "activeUsersByPlatform", "appActiveUsers");
        assertThat(requests).allSatisfy(request -> assertThat(request.endDate()).isEqualTo("yesterday"));
        // 1·7·28일 활성 사용자는 어제 하루로만 묻는다. 기간으로 물으면 날마다의 값을 더한 숫자가 온다(11/27 MAU 4,072)
        assertThat(requests).filteredOn(request -> ROLLING_REQUESTS.contains(request.name()))
                .allSatisfy(request -> {
                    assertThat(request.startDate()).isEqualTo("yesterday");
                    assertThat(request.metrics()).containsExactly("active1DayUsers", "active7DayUsers", "active28DayUsers");
                });
        assertThat(requests).filteredOn(request -> !ROLLING_REQUESTS.contains(request.name()))
                .allSatisfy(request -> {
                    assertThat(request.startDate()).isEqualTo("28daysAgo");
                    assertThat(request.metrics()).doesNotContain("active1DayUsers", "active7DayUsers", "active28DayUsers");
                });
        assertThat(requests.get(0).dimensions()).isEmpty();
        assertThat(requests.get(1).dimensions()).isEmpty();
        assertThat(requests.get(1).metrics()).containsExactly("newUsers", "activeUsers");
        assertThat(requests.get(2).dimensions()).containsExactly("date");
        assertThat(requests.get(2).metrics()).containsExactly("activeUsers");
        assertThat(requests.get(3).dimensions()).containsExactly("unifiedScreenName");
        assertThat(requests.get(3).metrics()).containsExactly("screenPageViews");
        assertThat(requests.get(3).orderByMetric()).isEqualTo("screenPageViews");
        assertThat(requests.get(3).limit()).isEqualTo(10);
        assertThat(requests.get(4).dimensions()).containsExactly("platform");
        assertThat(requests.get(5).dimensions()).containsExactly("appVersion");
        assertThat(requests.get(5).limit()).isEqualTo(10);
        assertThat(requests.get(6).dimensions()).containsExactly("platform");
        assertThat(requests.get(6).inListFilter()).isNull();
        // 앱 전체는 Android·iOS로 걸러 따로 센다(두 기기를 쓰는 사람을 1명으로)
        assertThat(requests.get(7).dimensions()).isEmpty();
        assertThat(requests.get(7).inListFilter())
                .isEqualTo(new Ga4ReportRequest.InListFilter("platform", List.of("Android", "iOS")));
    }

    @DisplayName("플랫폼 값은 대소문자를 가리지 않고 맞추고, 행이 없는 플랫폼·앱 전체는 0")
    @Test
    void activeUsersByPlatformMissingRows() {
        given(client.runReport(eq("123456789"), any())).willAnswer(invocation -> {
            Ga4ReportRequest request = invocation.getArgument(1);
            if (request.name().equals("activeUsersByPlatform")) {
                return new Ga4Report("Asia/Seoul", List.of(new Row(List.of("IOS"), List.of(7L, 8L, 9L))));
            }
            return new Ga4Report("Asia/Seoul", List.of());
        });

        Ga4MetricsResponse response = service(CONNECTED).getMetrics("7");

        assertThat(response.activeUsersByPlatform()).isEqualTo(new ActiveUsersByPlatform(
                ActiveUserCounts.ZERO, ActiveUserCounts.ZERO, new ActiveUserCounts(7, 8, 9), ActiveUserCounts.ZERO));
    }

    @DisplayName("이름은 캐시 뒤에 붙인다: 관리자가 이름을 바꾸면 GA4를 다시 부르지 않아도 다음 응답에 바로 보인다")
    @Test
    void displayNamesAreAppliedAfterCache() {
        answerWith("Asia/Seoul");
        AtomicReference<Ga4DisplayNames> names = new AtomicReference<>(Ga4DisplayNames.defaultsOnly());
        Ga4MetricsService service = new Ga4MetricsService(CONNECTED, client, clock, names::get);

        Ga4MetricsResponse before = service.getMetrics("28");
        assertThat(before.topScreens().get(0).displayName()).isEqualTo("[웹] Screen12");
        assertThat(before.platforms().get(0).displayName()).isEqualTo("앱(iOS)");

        names.set(Ga4DisplayNames.of(List.of(
                Ga4DisplayName.builder().valueType(Ga4ValueType.SCREEN).rawValue("Screen12")
                        .displayName("[웹] 첫 화면").sourceType(Ga4SourceType.WEB).updatedBy(1L).build(),
                Ga4DisplayName.builder().valueType(Ga4ValueType.PLATFORM).rawValue("iOS")
                        .displayName("아이폰 앱").updatedBy(1L).build())));
        Ga4MetricsResponse after = service.getMetrics("28");

        assertThat(after.fetchedAt()).isEqualTo(before.fetchedAt());
        assertThat(after.topScreens().get(0).displayName()).isEqualTo("[웹] 첫 화면");
        assertThat(after.platforms().get(0).displayName()).isEqualTo("아이폰 앱");
        assertThat(after.topScreens().get(0).screenName()).isEqualTo("Screen12");
        verify(client, times(8)).runReport(eq("123456789"), any());
    }

    @DisplayName("최근 값: 연결 전이면 enabled=false, 연결되면 90일·상위 200개를 종류별로 1시간 캐시한다")
    @Test
    void recentValues() {
        assertThat(service(new Ga4Properties(null, null)).getRecentValues(Ga4ValueType.SCREEN).enabled()).isFalse();

        given(client.runReport(eq("123456789"), any())).willAnswer(invocation -> new Ga4Report("Asia/Seoul", List.of(
                new Row(List.of("index"), List.of(5L)), new Row(List.of("(main)/(tabs)"), List.of(40L)))));
        Ga4MetricsService service = service(CONNECTED);

        Ga4RecentValues screens = service.getRecentValues(Ga4ValueType.SCREEN);
        service.getRecentValues(Ga4ValueType.SCREEN);
        service.getRecentValues(Ga4ValueType.APP_VERSION);

        assertThat(screens.enabled()).isTrue();
        assertThat(screens.endDate()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(screens.startDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(screens.values()).extracting(Ga4RecentValues.ValueCount::rawValue, Ga4RecentValues.ValueCount::count)
                .containsExactly(tuple("(main)/(tabs)", 40L), tuple("index", 5L));
        List<Ga4ReportRequest> requests = sentRequests(2);
        assertThat(requests.get(0).name()).isEqualTo("recentScreens");
        assertThat(requests.get(0).startDate()).isEqualTo("90daysAgo");
        assertThat(requests.get(0).dimensions()).containsExactly("unifiedScreenName");
        assertThat(requests.get(0).metrics()).containsExactly("screenPageViews");
        assertThat(requests.get(0).limit()).isEqualTo(200);
        assertThat(requests.get(1).name()).isEqualTo("recentAppVersions");
        assertThat(requests.get(1).dimensions()).containsExactly("appVersion");
        assertThat(requests.get(1).metrics()).containsExactly("activeUsers");
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
        assertThat(sentRequests(8)).filteredOn(request -> !ROLLING_REQUESTS.contains(request.name()))
                .allSatisfy(request -> assertThat(request.startDate()).isEqualTo("7daysAgo"));
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
        assertThat(response.activeUsersByPlatform()).isEqualTo(new ActiveUsersByPlatform(
                ActiveUserCounts.ZERO, ActiveUserCounts.ZERO, ActiveUserCounts.ZERO, ActiveUserCounts.ZERO));
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

        assertThat(cached).isEqualTo(first);
        assertThat(cached.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T22:10:00+09:00"));
        verify(client, times(8)).runReport(eq("123456789"), any());

        service.getMetrics("7");
        verify(client, times(16)).runReport(eq("123456789"), any());

        clock.advance(Duration.ofMinutes(1));
        Ga4MetricsResponse refreshed = service.getMetrics("28");
        assertThat(refreshed.fetchedAt()).isEqualTo(OffsetDateTime.parse("2026-11-19T23:10:00+09:00"));
        verify(client, times(24)).runReport(eq("123456789"), any());
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
        assertThat(calls).hasValue(3 + 8);
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

    /** 1·7·28일 활성 사용자를 묻는 요청(어제 하루 범위) */
    private static final Set<String> ROLLING_REQUESTS = Set.of("summary", "activeUsersByPlatform", "appActiveUsers");

    /** 요청 이름에 맞춘 가짜 GA4 보고서. 정렬은 일부러 섞어 둔다. */
    private static Ga4Report report(Ga4ReportRequest request, String timeZone) {
        List<Row> rows = new ArrayList<>();
        switch (request.name()) {
            case "summary" -> rows.add(new Row(List.of(), List.of(140L, 470L, 1100L)));
            case "periodTotals" -> rows.add(new Row(List.of(), List.of(85L, 1002L)));
            case "activeUsersByPlatform" -> {
                rows.add(new Row(List.of("web"), List.of(30L, 90L, 200L)));
                rows.add(new Row(List.of("Android"), List.of(50L, 200L, 500L)));
                rows.add(new Row(List.of("iOS"), List.of(80L, 230L, 520L)));
            }
            case "appActiveUsers" -> rows.add(new Row(List.of(), List.of(120L, 400L, 950L)));
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
