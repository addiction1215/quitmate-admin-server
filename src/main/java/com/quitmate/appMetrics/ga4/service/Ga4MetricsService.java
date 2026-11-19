package com.quitmate.appMetrics.ga4.service;

import com.quitmate.appMetrics.common.AppMetricsCache;
import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.AppVersion;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.DailyActiveUsers;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Platform;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Screen;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Summary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 「앱 지표」 GA4 섹션(KAN-218). GA4 Data API {@code runReport}를 다섯 번 불러 한 응답으로 묶는다.
 *
 * <ul>
 *   <li>기간: 끝 날짜는 GA4 속성 보고 시간대 기준 어제, 시작은 끝에서 period-1일 전.
 *       GA4에는 {@code {period}daysAgo ~ yesterday}로 보내 GA4가 자기 시간대로 풀게 하고,
 *       응답 날짜는 응답 metadata의 시간대로 같은 날을 계산한다.</li>
 *   <li>연결 설정(속성 ID·키 파일 경로)이 없으면 GA4를 부르지 않고 enabled=false.</li>
 *   <li>성공한 결과만 기간별로 1시간 캐시한다. 실패({@link Ga4UnavailableException}, 503)는 담지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
public class Ga4MetricsService {

    static final Duration CACHE_TTL = Duration.ofHours(1);
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final int TOP_SCREENS = 10;
    static final int TOP_APP_VERSIONS = 10;
    static final String END_DATE = "yesterday";

    // GA4 Data API 차원·지표 API 이름(https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema)
    static final String DATE = "date";
    static final String SCREEN_NAME = "unifiedScreenName";
    static final String PLATFORM = "platform";
    static final String APP_VERSION = "appVersion";
    static final String ACTIVE_USERS = "activeUsers";
    static final String SCREEN_PAGE_VIEWS = "screenPageViews";
    static final List<String> SUMMARY_METRICS =
            List.of("active1DayUsers", "active7DayUsers", "active28DayUsers", "newUsers", ACTIVE_USERS);

    private static final String CONSOLE_URL = "https://analytics.google.com/analytics/web/#/p%s/reports/intelligenthome";
    private static final DateTimeFormatter GA4_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final Ga4Properties properties;
    private final Ga4DataApiClient client;
    private final Clock clock;
    private final AppMetricsCache<Ga4MetricsResponse> cache;

    @Autowired
    public Ga4MetricsService(Ga4Properties properties, Ga4DataApiClient client) {
        this(properties, client, Clock.system(KST));
    }

    Ga4MetricsService(Ga4Properties properties, Ga4DataApiClient client, Clock clock) {
        this.properties = properties;
        this.client = client;
        this.clock = clock;
        this.cache = new AppMetricsCache<>(CACHE_TTL, clock);
    }

    /**
     * @param periodValue 쿼리 period 그대로. 7·28·90이 아니면 400
     * @throws Ga4UnavailableException GA4에서 지표를 받지 못했으면(503)
     */
    public Ga4MetricsResponse getMetrics(String periodValue) {
        AppMetricsPeriod period = AppMetricsPeriod.from(periodValue);
        if (!properties.isConfigured()) {
            if (properties.hasAnyValue()) {
                log.warn("GA4 설정에 속성 ID(숫자)·키 파일 경로 중 빠지거나 형식이 틀린 값이 있어 '연결 전'으로 응답합니다.");
            }
            return Ga4MetricsResponse.disabled(period, now());
        }
        String propertyId = properties.normalizedPropertyId().orElseThrow();
        return cache.get(period, () -> fetch(period, propertyId));
    }

    private Ga4MetricsResponse fetch(AppMetricsPeriod period, String propertyId) {
        OffsetDateTime fetchedAt = now();
        String start = period.days() + "daysAgo";

        Ga4Report summary = client.runReport(propertyId,
                request("summary", start, List.of(), SUMMARY_METRICS, null, null));
        Ga4Report daily = client.runReport(propertyId,
                request("dailyActiveUsers", start, List.of(DATE), List.of(ACTIVE_USERS), null, null));
        Ga4Report screens = client.runReport(propertyId,
                request("topScreens", start, List.of(SCREEN_NAME), List.of(SCREEN_PAGE_VIEWS), SCREEN_PAGE_VIEWS, TOP_SCREENS));
        Ga4Report platforms = client.runReport(propertyId,
                request("platforms", start, List.of(PLATFORM), List.of(ACTIVE_USERS), ACTIVE_USERS, null));
        Ga4Report appVersions = client.runReport(propertyId,
                request("appVersions", start, List.of(APP_VERSION), List.of(ACTIVE_USERS), ACTIVE_USERS, TOP_APP_VERSIONS));

        ZoneId zone = zoneOf(summary.timeZone());
        LocalDate endDate = fetchedAt.atZoneSameInstant(zone).toLocalDate().minusDays(1);
        LocalDate startDate = period.startDate(endDate);

        return new Ga4MetricsResponse(
                true,
                period.days(),
                startDate,
                endDate,
                zone.getId(),
                fetchedAt,
                CONSOLE_URL.formatted(propertyId),
                summaryOf(summary),
                dailyOf(daily, startDate, endDate),
                top(screens, row -> new Screen(row.dimension(0), row.metric(0)), Screen::views, Screen::screenName, TOP_SCREENS),
                top(platforms, row -> new Platform(row.dimension(0), row.metric(0)), Platform::activeUsers, Platform::platform, Integer.MAX_VALUE),
                top(appVersions, row -> new AppVersion(row.dimension(0), row.metric(0)), AppVersion::activeUsers, AppVersion::appVersion, TOP_APP_VERSIONS)
        );
    }

    private static Ga4ReportRequest request(String name, String start, List<String> dimensions, List<String> metrics,
                                            String orderByMetric, Integer limit) {
        return new Ga4ReportRequest(name, start, END_DATE, dimensions, metrics, orderByMetric, limit);
    }

    /** 차원 없이 부른 보고서는 기간 전체 한 줄. 데이터가 없어 행이 없으면 모두 0 */
    private static Summary summaryOf(Ga4Report report) {
        if (report.rows().isEmpty()) {
            return new Summary(0, 0, 0, 0, 0);
        }
        Ga4Report.Row row = report.rows().get(0);
        return new Summary(row.metric(0), row.metric(1), row.metric(2), row.metric(3), row.metric(4));
    }

    /** 기간의 날짜마다 한 줄. GA4가 주지 않은 날(활성 사용자 0)은 0으로 채운다. 기간 밖 날짜는 버린다. */
    private static List<DailyActiveUsers> dailyOf(Ga4Report report, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> byDate = new HashMap<>();
        for (Ga4Report.Row row : report.rows()) {
            byDate.merge(parseDate(row.dimension(0)), row.metric(0), Long::sum);
        }
        List<DailyActiveUsers> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            days.add(new DailyActiveUsers(date, byDate.getOrDefault(date, 0L)));
        }
        return days;
    }

    /** 값 내림차순(같으면 이름 순), 최대 {@code limit}개 */
    private static <T> List<T> top(Ga4Report report, Function<Ga4Report.Row, T> mapper,
                                   Function<T, Long> value, Function<T, String> name, int limit) {
        return report.rows().stream()
                .map(mapper)
                .sorted(Comparator.comparing(value, Comparator.reverseOrder())
                        .thenComparing(name, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, GA4_DATE);
        } catch (DateTimeParseException e) {
            throw new Ga4UnavailableException("GA4 응답의 날짜 형식이 예상과 다릅니다(dailyActiveUsers).", e);
        }
    }

    /** GA4 속성 보고 시간대. 응답에 없거나 읽지 못하면 한국 시간 */
    private static ZoneId zoneOf(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return KST;
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException e) {
            log.warn("GA4 응답의 시간대를 읽지 못해 한국 시간으로 계산합니다.");
            return KST;
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
