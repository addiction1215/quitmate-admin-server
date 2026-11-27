package com.quitmate.appMetrics.ga4.service;

import com.quitmate.appMetrics.common.AppMetricsCache;
import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.ga4.client.Ga4DataApiClient;
import com.quitmate.appMetrics.ga4.client.Ga4Report;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest;
import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.appMetrics.ga4.client.Ga4ReportRequest.InListFilter;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNameReader;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNames;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUserCounts;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.ActiveUsersByPlatform;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.AppVersion;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.DailyActiveUsers;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Platform;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Screen;
import com.quitmate.appMetrics.ga4.service.response.Ga4MetricsResponse.Summary;
import com.quitmate.appMetrics.ga4.service.response.Ga4RecentValues;
import com.quitmate.appMetrics.ga4.service.response.Ga4RecentValues.ValueCount;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 「앱 지표」 GA4 섹션(KAN-218, KAN-230). GA4 Data API {@code runReport}를 여덟 번 불러 한 응답으로 묶는다.
 *
 * <ul>
 *   <li>기간: 끝 날짜는 GA4 속성 보고 시간대 기준 어제, 시작은 끝에서 period-1일 전.
 *       GA4에는 {@code {period}daysAgo ~ yesterday}로 보내 GA4가 자기 시간대로 풀게 하고,
 *       응답 날짜는 응답 metadata의 시간대로 같은 날을 계산한다.</li>
 *   <li>DAU·WAU·MAU({@code active1DayUsers}·{@code active7DayUsers}·{@code active28DayUsers})는
 *       {@code yesterday ~ yesterday} 하루로 따로 묻는다(KAN-230). 기간 전체로 물으면 날마다의 값을 더한 숫자가 와서
 *       11/27 운영 화면에서 MAU가 4,072명(28일 × 하루 약 145명)으로 보였다. 이 지표를 같이 넣은 요청은
 *       {@code activeUsers}도 날마다 더해져서, 기간 전체 활성 사용자·신규 사용자는 기간 범위로 따로 묻는다.</li>
 *   <li>DAU·WAU·MAU는 플랫폼별로도 묻고, 앱 전체는 platform이 Android·iOS인 사람으로 걸러 따로 묻는다
 *       (두 기기를 다 쓰는 사람을 1명으로 세려고. Android + iOS를 더하면 2명이 된다).</li>
 *   <li>연결 설정(속성 ID·키 파일 경로)이 없으면 GA4를 부르지 않고 enabled=false.</li>
 *   <li>성공한 결과만 기간별로 1시간 캐시한다. 실패({@link Ga4UnavailableException}, 503)는 담지 않는다.</li>
 *   <li>화면·플랫폼·버전 이름은 캐시한 숫자에 요청마다 붙인다. 관리자가 이름을 바꾸면 캐시와 상관없이 바로 보인다.</li>
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
    /** 이름 없는 값 찾기: 최근 90일, 화면·버전은 상위 200개까지(KAN-230) */
    static final AppMetricsPeriod RECENT_PERIOD = AppMetricsPeriod.DAYS_90;
    static final int RECENT_LIMIT = 200;

    // GA4 Data API 차원·지표 API 이름(https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema)
    static final String DATE = "date";
    static final String SCREEN_NAME = "unifiedScreenName";
    static final String PLATFORM = "platform";
    static final String APP_VERSION = "appVersion";
    static final String ACTIVE_USERS = "activeUsers";
    static final String SCREEN_PAGE_VIEWS = "screenPageViews";
    static final String NEW_USERS = "newUsers";
    /** 끝 날짜 기준 1·7·28일 활성 사용자. 반드시 yesterday 하루 범위로 묻는다 */
    static final List<String> ROLLING_ACTIVE_USERS = List.of("active1DayUsers", "active7DayUsers", "active28DayUsers");
    static final List<String> PERIOD_TOTALS = List.of(NEW_USERS, ACTIVE_USERS);
    static final String ANDROID = "Android";
    static final String IOS = "iOS";
    static final String WEB = "web";

    private static final String CONSOLE_URL = "https://analytics.google.com/analytics/web/#/p%s/reports/intelligenthome";
    private static final DateTimeFormatter GA4_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final Ga4Properties properties;
    private final Ga4DataApiClient client;
    private final Clock clock;
    private final Supplier<Ga4DisplayNames> displayNames;
    private final AppMetricsCache<Ga4MetricsResponse> cache;
    private final Map<Ga4ValueType, AppMetricsCache<Ga4RecentValues>> recentCaches = new EnumMap<>(Ga4ValueType.class);

    @Autowired
    public Ga4MetricsService(Ga4Properties properties, Ga4DataApiClient client, Ga4DisplayNameReader displayNameReader) {
        this(properties, client, Clock.system(KST), displayNameReader::current);
    }

    /** 테스트용: 기본 이름표만 쓴다 */
    Ga4MetricsService(Ga4Properties properties, Ga4DataApiClient client, Clock clock) {
        this(properties, client, clock, Ga4DisplayNames::defaultsOnly);
    }

    Ga4MetricsService(Ga4Properties properties, Ga4DataApiClient client, Clock clock,
                      Supplier<Ga4DisplayNames> displayNames) {
        this.properties = properties;
        this.client = client;
        this.clock = clock;
        this.displayNames = displayNames;
        this.cache = new AppMetricsCache<>(CACHE_TTL, clock);
        for (Ga4ValueType type : Ga4ValueType.values()) {
            recentCaches.put(type, new AppMetricsCache<>(CACHE_TTL, clock));
        }
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
        return cache.get(period, () -> fetch(period, propertyId)).withDisplayNames(displayNames.get());
    }

    /**
     * 최근 90일 GA4에 들어온 한 종류의 원래 값(KAN-230 이름 없는 값 찾기). 종류별로 1시간 캐시한다.
     *
     * @throws Ga4UnavailableException GA4에서 받지 못했으면(503)
     */
    public Ga4RecentValues getRecentValues(Ga4ValueType type) {
        if (!properties.isConfigured()) {
            return Ga4RecentValues.disabled(now());
        }
        String propertyId = properties.normalizedPropertyId().orElseThrow();
        return recentCaches.get(type).get(RECENT_PERIOD, () -> fetchRecent(type, propertyId));
    }

    private Ga4RecentValues fetchRecent(Ga4ValueType type, String propertyId) {
        OffsetDateTime fetchedAt = now();
        String start = RECENT_PERIOD.days() + "daysAgo";
        Ga4ReportRequest request = switch (type) {
            case SCREEN -> request("recentScreens", start, List.of(SCREEN_NAME), List.of(SCREEN_PAGE_VIEWS),
                    SCREEN_PAGE_VIEWS, RECENT_LIMIT);
            case PLATFORM -> request("recentPlatforms", start, List.of(PLATFORM), List.of(ACTIVE_USERS),
                    ACTIVE_USERS, RECENT_LIMIT);
            case APP_VERSION -> request("recentAppVersions", start, List.of(APP_VERSION), List.of(ACTIVE_USERS),
                    ACTIVE_USERS, RECENT_LIMIT);
        };
        Ga4Report report = client.runReport(propertyId, request);
        LocalDate endDate = fetchedAt.atZoneSameInstant(zoneOf(report.timeZone())).toLocalDate().minusDays(1);
        List<ValueCount> values = top(report, row -> new ValueCount(row.dimension(0), row.metric(0)),
                ValueCount::count, ValueCount::rawValue, RECENT_LIMIT);
        return new Ga4RecentValues(true, RECENT_PERIOD.startDate(endDate), endDate, fetchedAt, values);
    }

    private Ga4MetricsResponse fetch(AppMetricsPeriod period, String propertyId) {
        OffsetDateTime fetchedAt = now();
        String start = period.days() + "daysAgo";

        Ga4Report summary = client.runReport(propertyId,
                request("summary", END_DATE, List.of(), ROLLING_ACTIVE_USERS, null, null));
        Ga4Report totals = client.runReport(propertyId,
                request("periodTotals", start, List.of(), PERIOD_TOTALS, null, null));
        Ga4Report daily = client.runReport(propertyId,
                request("dailyActiveUsers", start, List.of(DATE), List.of(ACTIVE_USERS), null, null));
        Ga4Report screens = client.runReport(propertyId,
                request("topScreens", start, List.of(SCREEN_NAME), List.of(SCREEN_PAGE_VIEWS), SCREEN_PAGE_VIEWS, TOP_SCREENS));
        Ga4Report platforms = client.runReport(propertyId,
                request("platforms", start, List.of(PLATFORM), List.of(ACTIVE_USERS), ACTIVE_USERS, null));
        Ga4Report appVersions = client.runReport(propertyId,
                request("appVersions", start, List.of(APP_VERSION), List.of(ACTIVE_USERS), ACTIVE_USERS, TOP_APP_VERSIONS));
        Ga4Report byPlatform = client.runReport(propertyId,
                request("activeUsersByPlatform", END_DATE, List.of(PLATFORM), ROLLING_ACTIVE_USERS, null, null));
        Ga4Report app = client.runReport(propertyId, new Ga4ReportRequest("appActiveUsers", END_DATE, END_DATE,
                List.of(), ROLLING_ACTIVE_USERS, null, null, new InListFilter(PLATFORM, List.of(ANDROID, IOS))));

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
                summaryOf(summary, totals),
                dailyOf(daily, startDate, endDate),
                top(screens, row -> new Screen(row.dimension(0), row.metric(0)), Screen::views, Screen::screenName, TOP_SCREENS),
                top(platforms, row -> new Platform(row.dimension(0), row.metric(0)), Platform::activeUsers, Platform::platform, Integer.MAX_VALUE),
                top(appVersions, row -> new AppVersion(row.dimension(0), row.metric(0)), AppVersion::activeUsers, AppVersion::appVersion, TOP_APP_VERSIONS),
                byPlatformOf(byPlatform, app)
        );
    }

    private static Ga4ReportRequest request(String name, String start, List<String> dimensions, List<String> metrics,
                                            String orderByMetric, Integer limit) {
        return new Ga4ReportRequest(name, start, END_DATE, dimensions, metrics, orderByMetric, limit);
    }

    /**
     * 차원 없이 부른 보고서는 한 줄. 데이터가 없어 행이 없으면 0.
     *
     * @param rolling 어제 하루 범위의 1·7·28일 활성 사용자
     * @param totals  기간 범위의 신규·활성 사용자
     */
    private static Summary summaryOf(Ga4Report rolling, Ga4Report totals) {
        ActiveUserCounts counts = countsOf(rolling.rows().isEmpty() ? null : rolling.rows().get(0));
        long newUsers = totals.rows().isEmpty() ? 0 : totals.rows().get(0).metric(0);
        long activeUsers = totals.rows().isEmpty() ? 0 : totals.rows().get(0).metric(1);
        return new Summary(counts.dau(), counts.wau(), counts.mau(), newUsers, activeUsers);
    }

    /**
     * @param byPlatform 플랫폼별 1·7·28일 활성 사용자(어제 하루 범위)
     * @param app        Android·iOS로 거른 1·7·28일 활성 사용자(어제 하루 범위)
     */
    private static ActiveUsersByPlatform byPlatformOf(Ga4Report byPlatform, Ga4Report app) {
        return new ActiveUsersByPlatform(
                countsOf(app.rows().isEmpty() ? null : app.rows().get(0)),
                platformCounts(byPlatform, ANDROID),
                platformCounts(byPlatform, IOS),
                platformCounts(byPlatform, WEB));
    }

    /** GA4 플랫폼 값은 대소문자를 가리지 않고 맞춘다. 그 플랫폼 행이 없으면 0 */
    private static ActiveUserCounts platformCounts(Ga4Report report, String platform) {
        return report.rows().stream()
                .filter(row -> platform.equalsIgnoreCase(row.dimension(0)))
                .findFirst()
                .map(Ga4MetricsService::countsOf)
                .orElse(ActiveUserCounts.ZERO);
    }

    /** 지표 순서는 {@link #ROLLING_ACTIVE_USERS}와 같다. 행이 없으면 0 */
    private static ActiveUserCounts countsOf(Ga4Report.Row row) {
        if (row == null) {
            return ActiveUserCounts.ZERO;
        }
        return new ActiveUserCounts(row.metric(0), row.metric(1), row.metric(2));
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
